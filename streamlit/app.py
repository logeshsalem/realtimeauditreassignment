#!/usr/bin/env python3
"""
Flask service: receive auditors + stores JSON from Spring Boot, predict one-to-one assignments
(one auditor -> at most one store) and return JSON result.

Environment:
 - Create an `apikey.env` file with keys API_KEY and FLASK_SECRET_KEY (or set them in env)
"""
import os
import json
import time
from typing import List, Dict, Any, Optional
from math import radians, sin, cos, sqrt, asin
from datetime import datetime
from functools import wraps

from dotenv import load_dotenv
from flask import Flask, request, jsonify

# Load environment variables
load_dotenv("apikey.env")
API_KEY = os.getenv("API_KEY")
FLASK_SECRET_KEY = os.getenv("FLASK_SECRET_KEY", "dev-secret")

# Configurable parameters
ESTIMATED_HOURS_PER_STORE = float(os.getenv("ESTIMATED_HOURS_PER_STORE", "4.0"))
DEFAULT_CAPACITY_FOR_AVAILABLE = float(os.getenv("DEFAULT_CAPACITY_FOR_AVAILABLE", "40.0"))
AUTO_ID_START = {"auditor": 1000, "store": 2000}
AUDITOR_STATUS_MAP = {
    "AVAILABLE": "Available",
    "ON_LEAVE": "Unavailable",
    "UNAVAILABLE": "Unavailable",
    "AVAILABLE_PART_TIME": "Available"
}
STORE_STATUS_MAP = {
    "OPEN": "Open",
    "CLOSED": "Closed",
    "OWNERSHIP_CHANGE": "Closed",
    "UNDER_MAINTENANCE": "Closed"
}

# Flask app
app = Flask(__name__)
app.config["SECRET_KEY"] = FLASK_SECRET_KEY

# ---- Utilities ----
def require_api_key(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        api_key = request.headers.get("X-API-Key")
        if not API_KEY:
            app.logger.warning("No API_KEY configured on server; rejecting request")
            return jsonify({"error": "Server misconfiguration: API_KEY not set"}), 500
        if not api_key or api_key != API_KEY:
            return jsonify({
                "error": "Invalid or missing API key",
                "status": "error",
                "code": "UNAUTHORIZED"
            }), 401
        return f(*args, **kwargs)
    return decorated_function

class ValidationError(Exception):
    pass

_id_counters = {"auditor": AUTO_ID_START["auditor"], "store": AUTO_ID_START["store"]}
def _next_id(kind: str) -> int:
    _id_counters[kind] += 1
    return _id_counters[kind]

def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Return distance between two lat/lon in kilometers (Haversine)."""
    R = 6371.0  # Earth radius in km
    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)
    a = sin(dlat/2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a))
    return R * c

def save_auditplan(result: dict, path: Optional[str] = None) -> bool:
    try:
        if path is None:
            path = os.path.join(os.path.dirname(__file__), "auditplan.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2, ensure_ascii=False)
        app.logger.info(f"Audit plan saved to {path}")
        return True
    except Exception as e:
        app.logger.error(f"Failed to save audit plan: {e}")
        return False

# ---- Adaptor: normalize incoming payloads to expected schema ----
def adapt_incoming_payload(raw: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
    """
    Normalize incoming payload to internal schema:
      auditors: { auditor_id(int), latitude(float), longitude(float),
                  availability_status(str), workloadCapacityHours(float),
                  currentAssignedHours(float), raw: original }
      stores:   { store_id(int), latitude(float), longitude(float),
                  store_status(str), raw: original }
    """
    auditors_in = raw.get("auditors") or raw.get("auditorList") or raw.get("employees") or []
    stores_in = raw.get("stores") or raw.get("storeList") or raw.get("locations") or []

    adapted_auditors = []
    for a in auditors_in:
        auditor_id = a.get("auditor_id") or a.get("id") or a.get("auditorId")
        if auditor_id is None:
            auditor_id = _next_id("auditor")
        try:
            auditor_id = int(auditor_id)
        except Exception:
            auditor_id = _next_id("auditor")

        lat = a.get("latitude") if a.get("latitude") is not None else a.get("homeLat") or a.get("locationLat") or a.get("lat")
        lon = a.get("longitude") if a.get("longitude") is not None else a.get("homeLon") or a.get("locationLon") or a.get("lon")
        try:
            lat = float(lat) if lat is not None else 0.0
            lon = float(lon) if lon is not None else 0.0
        except Exception:
            raise ValidationError("Invalid latitude/longitude for an auditor")

        raw_status = (a.get("availability_status") or a.get("availabilityStatus") or a.get("status") or "").strip()
        availability_status = AUDITOR_STATUS_MAP.get(raw_status.upper(), raw_status or "Unavailable")

        capacity = a.get("workloadCapacityHours") if a.get("workloadCapacityHours") is not None else a.get("capacityHours") or a.get("capacity")
        assigned = a.get("currentAssignedHours") if a.get("currentAssignedHours") is not None else a.get("assignedHours") or a.get("currentAssigned")
        try:
            if capacity is None:
                capacity_val = float(DEFAULT_CAPACITY_FOR_AVAILABLE) if availability_status == "Available" else 0.0
            else:
                capacity_val = float(capacity)
        except Exception:
            capacity_val = 0.0
        try:
            assigned_val = float(assigned) if assigned is not None else 0.0
        except Exception:
            assigned_val = 0.0

        adapted_auditors.append({
            "auditor_id": auditor_id,
            "latitude": lat,
            "longitude": lon,
            "availability_status": availability_status,
            "workloadCapacityHours": capacity_val,
            "currentAssignedHours": assigned_val,
            "raw": a
        })

    adapted_stores = []
    for s in stores_in:
        store_id = s.get("store_id") or s.get("id") or s.get("storeId")
        if store_id is None:
            store_id = _next_id("store")
        try:
            store_id = int(store_id)
        except Exception:
            store_id = _next_id("store")

        lat = s.get("latitude") if s.get("latitude") is not None else s.get("locationLat") or s.get("homeLat") or s.get("lat")
        lon = s.get("longitude") if s.get("longitude") is not None else s.get("locationLon") or s.get("homeLon") or s.get("lon")
        try:
            lat = float(lat) if lat is not None else 0.0
            lon = float(lon) if lon is not None else 0.0
        except Exception:
            raise ValidationError("Invalid latitude/longitude for a store")

        raw_status = (s.get("store_status") or s.get("storeStatus") or s.get("status") or "").strip()
        store_status = STORE_STATUS_MAP.get(raw_status.upper(), raw_status or "Closed")

        adapted_stores.append({
            "store_id": store_id,
            "latitude": lat,
            "longitude": lon,
            "store_status": store_status,
            "raw": s
        })

    return {"auditors": adapted_auditors, "stores": adapted_stores}

# ---- Validators ----
def validate_auditor(a: Dict[str, Any]) -> None:
    required = ["auditor_id", "latitude", "longitude", "availability_status", "workloadCapacityHours", "currentAssignedHours"]
    for r in required:
        if r not in a:
            raise ValidationError(f"Auditor missing required field: {r}")
    if not isinstance(a["auditor_id"], int):
        raise ValidationError("auditor_id must be an integer")
    if not isinstance(a["latitude"], (int, float)) or not -90 <= a["latitude"] <= 90:
        raise ValidationError("Invalid auditor latitude")
    if not isinstance(a["longitude"], (int, float)) or not -180 <= a["longitude"] <= 180:
        raise ValidationError("Invalid auditor longitude")
    if not isinstance(a["availability_status"], str):
        raise ValidationError("availability_status must be a string")
    if not isinstance(a["workloadCapacityHours"], (int, float)):
        raise ValidationError("workloadCapacityHours must be numeric")
    if not isinstance(a["currentAssignedHours"], (int, float)):
        raise ValidationError("currentAssignedHours must be numeric")

def validate_store(s: Dict[str, Any]) -> None:
    required = ["store_id", "latitude", "longitude", "store_status"]
    for r in required:
        if r not in s:
            raise ValidationError(f"Store missing required field: {r}")
    if not isinstance(s["store_id"], int):
        raise ValidationError("store_id must be an integer")
    if not isinstance(s["latitude"], (int, float)) or not -90 <= s["latitude"] <= 90:
        raise ValidationError("Invalid store latitude")
    if not isinstance(s["longitude"], (int, float)) or not -180 <= s["longitude"] <= 180:
        raise ValidationError("Invalid store longitude")
    if not isinstance(s["store_status"], str):
        raise ValidationError("store_status must be a string")

# ---- Core assignment logic (one-to-one) ----
def assign_stores_to_auditors(auditors: List[Dict[str, Any]], stores: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Assign OPEN stores to AVAILABLE auditors minimizing travel, with the constraint:
      - each auditor can be assigned to at most one store (one-to-one).
    Auditors with remaining_hours <= 0 are not eligible.
    """
    auditors_map: Dict[int, Dict[str, Any]] = {a["auditor_id"]: dict(a) for a in auditors}
    stores_map: Dict[int, Dict[str, Any]] = {s["store_id"]: dict(s) for s in stores}

    # Prepare auditors: remaining hours and assigned_store_ids (max one)
    for a in auditors_map.values():
        remaining = float(a.get("workloadCapacityHours", 0.0)) - float(a.get("currentAssignedHours", 0.0))
        a["remaining_hours"] = max(0.0, remaining)
        a["assigned_store_ids"] = []

    disruptions = []
    open_stores = [s for s in stores_map.values() if s["store_status"] == "Open"]

    # We'll greedily assign each store to the nearest eligible auditor,
    # and once an auditor is assigned, they are removed from eligibility.
    for store in open_stores:
        eligible = [
            a for a in auditors_map.values()
            if a["availability_status"] == "Available" and a["remaining_hours"] > 0.0 and len(a["assigned_store_ids"]) == 0
        ]
        if not eligible:
            store["assigned_auditor_id"] = None
            disruptions.append({
                "disruption_id": f"D{len(disruptions)+1:03d}",
                "store_id": store["store_id"],
                "event_type": "NO_AVAILABLE_AUDITOR",
                "triggered_on": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "reported_by": "system",
                "reassignment_status": "PENDING"
            })
            continue

        # compute nearest eligible auditor
        distances = []
        for a in eligible:
            dist_km = haversine_km(store["latitude"], store["longitude"], a["latitude"], a["longitude"])
            distances.append((a["auditor_id"], dist_km))
        distances.sort(key=lambda x: x[1])
        chosen_id, chosen_dist = distances[0]
        chosen = auditors_map[chosen_id]

        # allocate hours (use up to ESTIMATED_HOURS_PER_STORE or whatever remaining)
        alloc_hours = min(ESTIMATED_HOURS_PER_STORE, chosen["remaining_hours"])
        chosen["remaining_hours"] = max(0.0, chosen["remaining_hours"] - alloc_hours)
        chosen["assigned_store_ids"].append(store["store_id"])

        # enforce one-to-one by not allowing further assignments to this auditor:
        # already handled because len(assigned_store_ids) == 1 will block future eligibility

        store["assigned_auditor_id"] = chosen_id

        disruptions.append({
            "disruption_id": f"D{len(disruptions)+1:03d}",
            "store_id": store["store_id"],
            "event_type": "STORE_ASSIGNMENT",
            "triggered_on": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "reported_by": "system",
            "reassignment_status": "IMPLEMENTED",
            "assigned_auditor_id": chosen_id,
            "distance_km": round(chosen_dist, 4),
            "allocated_hours": round(alloc_hours, 3)
        })

    # Build auditors output
    output_auditors = []
    for a in auditors_map.values():
        used_hours = float(a.get("workloadCapacityHours", 0.0)) - a["remaining_hours"]
        predicted_assigned = float(a.get("currentAssignedHours", 0.0)) + used_hours
        output_auditors.append({
            "auditor_id": a["auditor_id"],
            "latitude": a["latitude"],
            "longitude": a["longitude"],
            "availability_status": a["availability_status"],
            "workloadCapacityHours": a["workloadCapacityHours"],
            "currentAssignedHours": round(predicted_assigned, 3),
            "remaining_hours": round(a["remaining_hours"], 3),
            "assigned_store_ids": a["assigned_store_ids"],
            "raw": a.get("raw")
        })

    # Build stores output
    output_stores = []
    for s in stores_map.values():
        output_stores.append({
            "store_id": s["store_id"],
            "latitude": s["latitude"],
            "longitude": s["longitude"],
            "store_status": s["store_status"],
            "assigned_auditor_id": s.get("assigned_auditor_id"),
            "raw": s.get("raw")
        })

    return {"auditors": output_auditors, "stores": output_stores, "disruptions": disruptions}

# ---- API route ----
@app.route("/api/process-assignments", methods=["POST"])
@require_api_key
def process_assignments():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No JSON data provided", "status": "error", "code": "INVALID_REQUEST"}), 400

        try:
            adapted = adapt_incoming_payload(data)
        except ValidationError as ve:
            return jsonify({"error": str(ve), "status": "error", "code": "ADAPTATION_ERROR"}), 400
        except Exception as e:
            app.logger.exception("Unexpected error during adaptation")
            return jsonify({"error": "Adaptation failed", "status": "error", "code": "ADAPTATION_ERROR", "details": str(e)}), 500

        auditors = adapted["auditors"]
        stores = adapted["stores"]

        try:
            for a in auditors:
                validate_auditor(a)
            for s in stores:
                validate_store(s)
        except ValidationError as ve:
            return jsonify({"error": str(ve), "status": "error", "code": "VALIDATION_ERROR"}), 400

        try:
            result = assign_stores_to_auditors(auditors, stores)
        except Exception as e:
            app.logger.exception("Assignment error")
            return jsonify({"error": "Assignment failed", "status": "error", "code": "ASSIGNMENT_ERROR", "details": str(e)}), 500

        saved = save_auditplan(result)
        if not saved:
            app.logger.warning("Failed to save auditplan to file; continuing response")

        app.logger.info("Assignment result: %s", json.dumps(result, default=str))

        return jsonify({"status": "success", "code": "SUCCESS", "data": result}), 200

    except Exception as e:
        app.logger.exception("Unhandled exception in process_assignments")
        return jsonify({"error": "Internal server error", "status": "error", "code": "INTERNAL_ERROR", "message": str(e)}), 500

@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({"status": "success", "message": "Service is running", "timestamp": datetime.now().isoformat()}), 200

if __name__ == "__main__":
    if not API_KEY:
        raise RuntimeError("API_KEY must be set in apikey.env or environment variables")
    print("Starting Flask server...")
    print("API endpoint available at http://0.0.0.0:5000/api/process-assignments")
    app.run(host="0.0.0.0", port=5000)
