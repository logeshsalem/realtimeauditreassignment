from flask import Flask, request, jsonify
from openai import OpenAI
import json
import os
import math
import traceback
from datetime import datetime

# ==================== CONFIGURATION ====================
app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False

# Initialize OpenAI client
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
if OPENAI_API_KEY:
    client = OpenAI(api_key=OPENAI_API_KEY)
else:
    client = None
    print("⚠️  WARNING: OPENAI_API_KEY not set. AI features will be disabled.")

# Constants
AUDITOR_CAPACITY_HOURS = 40.0
HOURS_PER_STORE_AUDIT = 4.0
MAX_STORES_PER_AUDITOR = 1  # ONE AUDITOR = ONE STORE ONLY

# ==================== UTILITY FUNCTIONS ====================

def calculate_distance(lat1, lon1, lat2, lon2):
    """
    Calculate distance between two points using Haversine formula.
    
    Args:
        lat1, lon1: Latitude and longitude of point 1
        lat2, lon2: Latitude and longitude of point 2
    
    Returns:
        Distance in kilometers
    """
    R = 6371  # Earth's radius in kilometers
    
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    
    a = (math.sin(dlat / 2) ** 2 + 
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * 
         math.sin(dlon / 2) ** 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    return R * c

def validate_request_data(data):
    """
    Validate incoming request data.
    
    Returns:
        tuple: (is_valid, error_message)
    """
    if not data:
        return False, "Request body is empty"
    
    if 'auditors' not in data:
        return False, "Missing 'auditors' field in request"
    
    if 'stores' not in data:
        return False, "Missing 'stores' field in request"
    
    if not isinstance(data['auditors'], list):
        return False, "'auditors' must be a list"
    
    if not isinstance(data['stores'], list):
        return False, "'stores' must be a list"
    
    if len(data['auditors']) == 0:
        return False, "Auditors list is empty"
    
    if len(data['stores']) == 0:
        return False, "Stores list is empty"
    
    # Validate auditor fields
    for auditor in data['auditors']:
        required_fields = ['auditor_id', 'latitude', 'longitude', 'availability_status']
        for field in required_fields:
            if field not in auditor:
                return False, f"Auditor missing required field: {field}"
    
    # Validate store fields
    for store in data['stores']:
        required_fields = ['store_id', 'latitude', 'longitude', 'store_status']
        for field in required_fields:
            if field not in store:
                return False, f"Store missing required field: {field}"
    
    return True, None

# ==================== ASSIGNMENT ALGORITHMS ====================

def assign_with_openai(auditors, stores):
    """
    Use OpenAI GPT-4 to intelligently assign auditors to stores.
    RULE: One auditor can only be assigned to ONE store.
    
    Args:
        auditors: List of auditor dictionaries
        stores: List of store dictionaries
    
    Returns:
        dict: Assignment result with 'assignments' key
    """
    if not client:
        raise Exception("OpenAI client not initialized. Check API key.")
    
    # Prepare structured prompt
    prompt = f"""You are an intelligent auditor assignment optimization system. Assign auditors to stores optimally.

AVAILABLE AUDITORS ({len(auditors)} total):
{json.dumps(auditors, indent=2)}

STORES TO AUDIT ({len(stores)} total):
{json.dumps(stores, indent=2)}

CRITICAL ASSIGNMENT RULES:
1. ⚠️ EACH AUDITOR CAN BE ASSIGNED TO ONLY ONE STORE (1:1 mapping)
2. ⚠️ Once an auditor is assigned, they CANNOT be assigned to any other store
3. Only assign to auditors with availability_status = "Available"
4. Prioritize geographic proximity - assign nearest available auditor to each store
5. If multiple unassigned stores exist and fewer auditors are available, assign to the closest stores first
6. Each store audit requires {HOURS_PER_STORE_AUDIT} hours

OPTIMIZATION GOALS:
- Minimize travel distance for each assignment
- Ensure each auditor is assigned to maximum ONE store
- Cover as many stores as possible with available auditors

OUTPUT FORMAT (JSON only, no markdown):
{{
  "assignments": [
    {{
      "store_id": 1,
      "assigned_auditor_id": 3,
      "distance_km": 25.4,
      "reason": "Nearest available auditor (one-to-one assignment)"
    }},
    {{
      "store_id": 2,
      "assigned_auditor_id": null,
      "distance_km": null,
      "reason": "All auditors already assigned"
    }}
  ]
}}

IMPORTANT: 
- Each auditor_id should appear in assigned_auditor_id field ONLY ONCE across all assignments
- If there are more stores than auditors, some stores will have assigned_auditor_id: null
- Provide ONLY the JSON output, no additional text or markdown."""

    try:
        print("📡 Calling OpenAI API for intelligent one-to-one assignment...")
        
        response = client.chat.completions.create(
            model="gpt-4",
            messages=[
                {
                    "role": "system", 
                    "content": "You are an expert operations research system specializing in one-to-one resource allocation. Each auditor can only be assigned to exactly one store. You always respond with valid JSON only."
                },
                {
                    "role": "user", 
                    "content": prompt
                }
            ],
            temperature=0.2,
            max_tokens=3000
        )
        
        # Extract and parse response
        ai_response = response.choices[0].message.content.strip()
        
        # Clean up markdown code blocks if present
        if "```json" in ai_response:
            ai_response = ai_response.split("```json")[1].split("```")[0].strip()
        elif "```" in ai_response:
            ai_response = ai_response.split("```")[1].split("```")[0].strip()
        
        result = json.loads(ai_response)
        
        # Validate one-to-one constraint
        assigned_auditors = set()
        for assignment in result.get('assignments', []):
            auditor_id = assignment.get('assigned_auditor_id')
            if auditor_id and auditor_id in assigned_auditors:
                print(f"⚠️ AI violated one-to-one rule! Auditor {auditor_id} assigned multiple times. Using fallback.")
                raise Exception("AI violated one-to-one constraint")
            if auditor_id:
                assigned_auditors.add(auditor_id)
        
        print(f"✅ OpenAI assignment completed: {len(result.get('assignments', []))} assignments (one-to-one)")
        return result
    
    except json.JSONDecodeError as e:
        print(f"❌ Failed to parse OpenAI response as JSON: {e}")
        print(f"Response was: {ai_response[:200]}...")
        raise Exception(f"AI response was not valid JSON: {str(e)}")
    
    except Exception as e:
        print(f"❌ OpenAI API error: {str(e)}")
        raise

def assign_with_greedy_algorithm(auditors, stores):
    """
    Fallback: Simple greedy algorithm based on distance.
    Assigns each store to the nearest available auditor.
    RULE: Each auditor can only be assigned to ONE store.
    
    Args:
        auditors: List of auditor dictionaries
        stores: List of store dictionaries
    
    Returns:
        dict: Assignment result with 'assignments' key
    """
    print("🔄 Using fallback greedy distance-based assignment (one-to-one)...")
    
    assignments = []
    assigned_auditors = set()  # Track which auditors are already assigned
    
    # Create a list of (store, distances_to_auditors) for sorting
    store_distance_pairs = []
    
    for store in stores:
        distances = []
        for auditor in auditors:
            if auditor['availability_status'] != 'Available':
                continue
            
            distance = calculate_distance(
                store['latitude'], store['longitude'],
                auditor['latitude'], auditor['longitude']
            )
            distances.append((auditor['auditor_id'], distance))
        
        # Sort auditors by distance for this store
        distances.sort(key=lambda x: x[1])
        store_distance_pairs.append((store, distances))
    
    # Sort stores by their minimum distance to any auditor (greedy approach)
    store_distance_pairs.sort(key=lambda x: x[1][0][1] if x[1] else float('inf'))
    
    # Assign each store to its nearest available auditor
    for store, sorted_auditors in store_distance_pairs:
        assigned = False
        
        # Try to find an unassigned auditor for this store
        for auditor_id, distance in sorted_auditors:
            if auditor_id not in assigned_auditors:
                # Assign this auditor to this store
                assigned_auditors.add(auditor_id)
                assignments.append({
                    "store_id": store['store_id'],
                    "assigned_auditor_id": auditor_id,
                    "distance_km": round(distance, 2),
                    "reason": "Nearest available auditor (one-to-one assignment)"
                })
                assigned = True
                print(f"  ✓ Store {store['store_id']} → Auditor {auditor_id} ({round(distance, 2)} km)")
                break
        
        # If no auditor available, store remains unassigned
        if not assigned:
            assignments.append({
                "store_id": store['store_id'],
                "assigned_auditor_id": None,
                "distance_km": None,
                "reason": "All auditors already assigned to other stores"
            })
            print(f"  ✗ Store {store['store_id']} → No auditor available")
    
    print(f"✅ Greedy assignment completed: {len([a for a in assignments if a['assigned_auditor_id']])} stores assigned")
    return {"assignments": assignments}

def perform_assignment(auditors, stores, use_ai=True):
    """
    Main assignment orchestrator. Tries AI first, falls back to greedy.
    RULE: One auditor = One store only.
    
    Args:
        auditors: List of auditor dictionaries
        stores: List of store dictionaries
        use_ai: Whether to attempt AI assignment first
    
    Returns:
        dict: Assignment result
    """
    if use_ai and client:
        try:
            return assign_with_openai(auditors, stores)
        except Exception as e:
            print(f"⚠️  AI assignment failed: {e}")
            print("⚠️  Falling back to greedy algorithm...")
    
    return assign_with_greedy_algorithm(auditors, stores)

# ==================== RESPONSE FORMATTING ====================

def format_spring_boot_response(auditors, stores, assignments):
    """
    Format the response in the structure expected by Spring Boot.
    
    Args:
        auditors: Original auditor list
        stores: Original store list
        assignments: Assignment results from AI/algorithm
    
    Returns:
        dict: Formatted response matching Spring Boot expectations
    """
    # Build lookup maps
    auditor_to_stores = {}
    store_to_auditor = {}
    
    for assignment in assignments['assignments']:
        store_id = assignment['store_id']
        auditor_id = assignment['assigned_auditor_id']
        
        store_to_auditor[store_id] = auditor_id
        
        if auditor_id:
            # Each auditor should only have ONE store
            if auditor_id not in auditor_to_stores:
                auditor_to_stores[auditor_id] = []
            auditor_to_stores[auditor_id].append(store_id)
    
    # Format auditors
    formatted_auditors = []
    for auditor in auditors:
        auditor_id = auditor['auditor_id']
        assigned_store_ids = auditor_to_stores.get(auditor_id, [])
        current_hours = len(assigned_store_ids) * HOURS_PER_STORE_AUDIT
        
        formatted_auditors.append({
            "auditor_id": auditor_id,
            "latitude": auditor['latitude'],
            "longitude": auditor['longitude'],
            "availability_status": auditor['availability_status'],
            "workloadCapacityHours": AUDITOR_CAPACITY_HOURS,
            "currentAssignedHours": current_hours,
            "remaining_hours": AUDITOR_CAPACITY_HOURS - current_hours,
            "assigned_store_ids": assigned_store_ids,
            "raw": auditor
        })
    
    # Format stores
    formatted_stores = []
    for store in stores:
        store_id = store['store_id']
        assigned_auditor_id = store_to_auditor.get(store_id)
        
        formatted_stores.append({
            "store_id": store_id,
            "latitude": store['latitude'],
            "longitude": store['longitude'],
            "store_status": store['store_status'],
            "assigned_auditor_id": assigned_auditor_id,
            "raw": store
        })
    
    return {
        "status": "success",
        "code": "SUCCESS",
        "data": {
            "auditors": formatted_auditors,
            "stores": formatted_stores
        }
    }

# ==================== API ENDPOINTS ====================

@app.route('/api/process-assignments', methods=['POST'])
def assign_auditors():
    """
    Main endpoint for auditor assignment.
    RULE: One auditor can only be assigned to ONE store.
    
    Expected Input:
        {
            "auditors": [...],
            "stores": [...]
        }
    
    Returns:
        JSON response with assignments in Spring Boot format
    """
    try:
        # Parse request
        data = request.get_json()
        
        print("=" * 60)
        print("📥 RECEIVED REQUEST")
        print("=" * 60)
        
        # Validate request
        is_valid, error_message = validate_request_data(data)
        if not is_valid:
            print(f"❌ Validation failed: {error_message}")
            return jsonify({
                "status": "error",
                "code": "INVALID_REQUEST",
                "message": error_message
            }), 400
        
        auditors = data['auditors']
        stores = data['stores']
        
        print(f"📊 Processing: {len(auditors)} auditors, {len(stores)} stores")
        print(f"⚠️  RULE: One auditor → One store only (1:1 mapping)")
        
        # Perform assignment
        assignments = perform_assignment(auditors, stores, use_ai=True)
        
        # Format response
        response = format_spring_boot_response(auditors, stores, assignments)
        
        print("=" * 60)
        print("📤 SENDING RESPONSE")
        print("=" * 60)
        print(json.dumps(response, indent=2))
        print("=" * 60)
        
        return jsonify(response), 200
    
    except Exception as e:
        print("=" * 60)
        print("❌ ERROR OCCURRED")
        print("=" * 60)
        print(f"Error: {str(e)}")
        print(traceback.format_exc())
        print("=" * 60)
        
        return jsonify({
            "status": "error",
            "code": "PROCESSING_ERROR",
            "message": str(e),
            "details": traceback.format_exc()
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "service": "auditor-assignment-ai",
        "assignment_rule": "One auditor → One store only",
        "ai_enabled": client is not None,
        "timestamp": datetime.now().isoformat()
    }), 200

@app.route('/', methods=['GET'])
def index():
    """Root endpoint with service information"""
    return jsonify({
        "service": "AI-Powered Auditor Assignment Service",
        "version": "1.0.0",
        "assignment_rule": "⚠️ One auditor can only be assigned to ONE store",
        "endpoints": {
            "POST /assign-auditors": "Main assignment endpoint",
            "GET /health": "Health check",
            "GET /": "Service information"
        },
        "ai_status": "enabled" if client else "disabled (no API key)",
        "documentation": "Send POST request to /assign-auditors with auditors and stores data"
    }), 200

# ==================== MAIN ====================

if __name__ == '__main__':
    print("=" * 60)
    print("🚀 AI AUDITOR ASSIGNMENT SERVICE")
    print("=" * 60)
    print(f"⚠️  RULE: One Auditor → One Store Only (1:1 Mapping)")
    print(f"OpenAI Integration: {'✅ Enabled' if client else '❌ Disabled (Set OPENAI_API_KEY)'}")
    print(f"Server: http://localhost:5000")
    print(f"Endpoint: POST /assign-auditors")
    print("=" * 60)
    print()
    
    # Run Flask app
    app.run(
        host='0.0.0.0',
        port=5000,
        debug=True
    )