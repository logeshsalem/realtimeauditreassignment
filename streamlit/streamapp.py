import streamlit as st
import requests
import pandas as pd

# --- Configuration ---
API_URL = "http://localhost:8080/api"

# --- Page Config ---
st.set_page_config(
    page_title="Management Dashboard",
    layout="wide",
    initial_sidebar_state="expanded"
)

st.title("Real-Time Audit Reassignment System")
st.markdown("For Auditors, Stores, and Audit Planning (Java Spring Boot Backend)")

# --- Sidebar Navigation ---
st.sidebar.title("Navigation")
page_selection = st.sidebar.radio(
    "Go to",
    ["Auditor Management", "Store Management", "Audit Plan Management"]
)

if 'newly_generated_plan' not in st.session_state:
    st.session_state.newly_generated_plan = None

# ==============================================================================
# ======================== AUDITOR MANAGEMENT PAGE =============================
# ==============================================================================

if page_selection == "Auditor Management":
    st.header("Auditor Management")
    
    st.sidebar.header("Auditor Actions")

    # Section 1: Create New Auditor
    st.sidebar.subheader("➕ Create New Auditor")
    with st.sidebar.form("create_auditor_form", clear_on_submit=True):
        name = st.text_input("Name")
        home_lat = st.number_input("Home Latitude", format="%.6f")
        home_lon = st.number_input("Home Longitude", format="%.6f")
        capacity = st.number_input("Workload Capacity (Hours)", min_value=0.0, step=0.5)
        initial_hours = st.number_input("Initial Assigned Hours", min_value=0.0, step=0.5, value=0.0)
        status_options = ["AVAILABLE", "UNAVAILABLE", "ON_LEAVE"]
        status = st.selectbox("Initial Status", status_options)
        if st.form_submit_button("Create Auditor"):
            payload = { "name": name, "homeLat": home_lat, "homeLon": home_lon, 
                        "workLoadCapacityHours": capacity, "currentAssignedHours": initial_hours, 
                        "availabilityStatus": status }
            try:
                response = requests.post(f"{API_URL}/auditor", json=payload)
                if response.status_code == 201: st.sidebar.success("Auditor Created!")
                else: st.sidebar.error(f"Failed. Code: {response.status_code}\n{response.text}")
            except requests.exceptions.RequestException as e: st.sidebar.error(f"Connection Error: {e}")

    st.sidebar.divider()

    # Section 2: Update Status
    st.sidebar.subheader("🔄 Update Status")
    status_update_id = st.sidebar.number_input("Auditor ID", min_value=1, step=1, key="auditor_status_id")
    new_status = st.sidebar.selectbox("New Status", status_options, key="update_status_box")
    if st.sidebar.button("Update Status"):
        try:
            response = requests.put(f"{API_URL}/auditor/{status_update_id}", params={"status": new_status})
            if response.status_code == 200: st.sidebar.success(f"Auditor {status_update_id} updated.")
            else: st.sidebar.error(f"Update failed. Code: {response.status_code}\n{response.text}")
        except requests.exceptions.RequestException as e: st.sidebar.error(f"Connection Error: {e}")

    st.sidebar.divider()
    
    # Section 3: Update Hours
    st.sidebar.subheader("⏱️ Update Assigned Hours")
    hours_update_id = st.sidebar.number_input("Auditor ID", min_value=1, step=1, key="hours_id")
    new_hours = st.sidebar.number_input("New Assigned Hours", min_value=0.0, step=0.5)
    if st.sidebar.button("Update Hours"):
        try:
            response = requests.put(f"{API_URL}/auditor/{hours_update_id}/hours", params={"hours": new_hours})
            if response.status_code == 200: st.sidebar.success(f"Auditor {hours_update_id} hours updated.")
            else: st.sidebar.error(f"Update failed. Code: {response.status_code}\n{response.text}")
        except requests.exceptions.RequestException as e: st.sidebar.error(f"Connection Error: {e}")

    # --- Main Content for Auditors ---
    tab1, tab2 = st.tabs(["📋 All Auditors", "✅ Available Auditors"])
    with tab1:
        st.subheader("List of All Auditors")
        if st.button("Refresh All Auditors"):
            try:
                response = requests.get(f"{API_URL}/auditors")
                if response.status_code == 200:
                    df = pd.DataFrame(response.json())
                    if not df.empty:
                        cols = ["id", "name", "availabilityStatus", "currentAssignedHours", "workLoadCapacityHours", "homeLat", "homeLon"]
                        st.dataframe(df[cols], use_container_width=True)
                    else: st.info("No auditors found.")
                else: st.error(f"Error fetching data: {response.status_code}")
            except requests.exceptions.RequestException as e: st.error(f"Connection Error: {e}")
    
    with tab2:
        st.subheader("List of Available Auditors")
        if st.button("Refresh Available Auditors"):
            try:
                response = requests.get(f"{API_URL}/auditors/available")
                if response.status_code == 200:
                    df = pd.DataFrame(response.json())
                    if not df.empty:
                        st.dataframe(df, use_container_width=True)
                    else:
                        st.info("No auditors are currently available.")
                else:
                    st.error(f"Error fetching data: {response.status_code}")
            except requests.exceptions.RequestException as e:
                st.error(f"Connection Error: {e}")


# ==============================================================================
# ========================== STORE MANAGEMENT PAGE =============================
# ==============================================================================

elif page_selection == "Store Management":
    st.header("Store Management")
    st.sidebar.header("Store Actions")

    # Section 1: Create New Store
    st.sidebar.subheader("➕ Create New Store")
    with st.sidebar.form("create_store_form", clear_on_submit=True):
        store_name = st.text_input("Store Name")
        store_address = st.text_input("Address")
        store_lat = st.number_input("Location Latitude", format="%.6f")
        store_lon = st.number_input("Location Longitude", format="%.6f")
        store_status_options = ["OPEN", "CLOSED"]
        store_status = st.selectbox("Initial Status", store_status_options)
        if st.form_submit_button("Create Store"):
            payload = { "name": store_name, "address": store_address, "locationLat": store_lat, 
                        "locationLon": store_lon, "storeStatus": store_status }
            try:
                response = requests.post(f"{API_URL}/store", json=payload)
                if response.status_code == 201: st.sidebar.success("Store Created!")
                else: st.sidebar.error(f"Failed. Code: {response.status_code}\n{response.text}")
            except requests.exceptions.RequestException as e: st.sidebar.error(f"Connection Error: {e}")

    st.sidebar.divider()

    # Section 2: Update Store Status
    st.sidebar.subheader("🔄 Update Store Status")
    store_update_id = st.sidebar.number_input("Store ID", min_value=1, step=1, key="store_status_id")
    new_store_status = st.sidebar.selectbox("New Status", store_status_options, key="update_store_status_box")
    if st.sidebar.button("Update Store Status"):
        try:
            response = requests.put(f"{API_URL}/store/{store_update_id}", params={"status": new_store_status})
            if response.status_code == 200: st.sidebar.success(f"Store {store_update_id} updated.")
            else: st.sidebar.error(f"Update failed. Code: {response.status_code}\n{response.text}")
        except requests.exceptions.RequestException as e: st.sidebar.error(f"Connection Error: {e}")
        
    # --- Main Content for Stores ---
    tab1, tab2 = st.tabs(["🏬 All Stores", "✅ Open Stores"])
    with tab1:
        st.subheader("List of All Stores")
        if st.button("Refresh All Stores List"):
            try:
                response = requests.get(f"{API_URL}/store")
                if response.status_code == 200:
                    df = pd.DataFrame(response.json())
                    if not df.empty: st.dataframe(df, use_container_width=True)
                    else: st.info("No stores found.")
                else: st.error(f"Error fetching data: {response.status_code}")
            except requests.exceptions.RequestException as e: st.error(f"Connection Error: {e}")
    with tab2:
        st.subheader("Currently Open Stores")
        if st.button("Refresh Open Stores List"):
            try:
                response = requests.get(f"{API_URL}/store/open")
                if response.status_code == 200:
                    df = pd.DataFrame(response.json())
                    if not df.empty: st.dataframe(df, use_container_width=True)
                    else: st.info("No stores are currently open.")
                else: st.error(f"Error fetching data: {response.status_code}")
            except requests.exceptions.RequestException as e: st.error(f"Connection Error: {e}")

# ==============================================================================
# ======================= AUDIT PLAN MANAGEMENT PAGE ===========================
# ==============================================================================

elif page_selection == "Audit Plan Management":
    st.header("Audit Plan Management")
    st.sidebar.header("Audit Plan Actions")

    # Section 1: Generate New Audit Plan
    st.sidebar.subheader("⚙️ Generate New Audit Plan")
    st.sidebar.markdown("Triggers the backend process to assign available auditors to open stores.")
    if st.sidebar.button("Generate and Save Plan"):
        with st.spinner("Generating plan..."):
            try:
                # Use the correct endpoint for automated generation
                response = requests.post(f"{API_URL}/process")
                if response.status_code == 200:
                    st.sidebar.success("New audit plan generated successfully!")
                    # Extract the list of assignments from the response
                    st.session_state.newly_generated_plan = response.json().get("assignments", [])
                else:
                    st.sidebar.error(f"Failed. Code: {response.status_code}\n{response.text}")
            except requests.exceptions.RequestException as e:
                st.sidebar.error(f"Connection Error: {e}")

    st.sidebar.divider()

    # Section 2: Update an Audit Plan
    st.sidebar.subheader("🔄 Update an Audit Plan")
    with st.sidebar.form("update_audit_plan_form"):
        plan_id = st.number_input("Audit Plan ID to Update", min_value=1, step=1)
        status_opts = ["PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELED", "DISRUPTED", "REASSIGNED"]
        priority_opts = ["LOW", "MEDIUM", "HIGH"]
        new_status = st.selectbox("New Audit Status", status_opts)
        new_priority = st.selectbox("New Audit Priority", priority_opts)
        
        if st.form_submit_button("Update Audit Plan"):
            payload = {"auditStatus": new_status, "auditPriority": new_priority}
            try:
                response = requests.put(f"{API_URL}/audit-plan/{plan_id}", json=payload)
                if response.status_code == 200:
                    st.sidebar.success(f"Plan {plan_id} updated!")
                else:
                    st.sidebar.error(f"Update failed. Code: {response.status_code}\n{response.text}")
            except requests.exceptions.RequestException as e:
                st.sidebar.error(f"Connection Error: {e}")
    
    # --- Main Content for Audit Plan ---
    tab1, tab2 = st.tabs(["📄 View Full Audit Plan", "🚀 Newly Generated Plan"])

    # --- THE FIX: The unnecessary flattening function has been REMOVED ---

    with tab1:
        st.subheader("Complete List of All Audit Plans")
        if st.button("Refresh Full Audit Plan"):
            try:
                response = requests.get(f"{API_URL}/audit-plans") 
                if response.status_code == 200:
                    data = response.json()
                    if data:
                        # The JSON response is already a flat list of objects, perfect for a DataFrame.
                        df = pd.DataFrame(data)
                        st.dataframe(df, use_container_width=True)
                    else:
                        st.info("No audit plans found.")
                else:
                    st.error(f"Error fetching data: {response.status_code}\n{response.text}")
            except requests.exceptions.RequestException as e:
                st.error(f"Connection Error: {e}")
    
    with tab2:
        st.subheader("Result from Last 'Generate Plan' Action")
        if st.session_state.newly_generated_plan:
            # The data in the session state is also already flat.
            df = pd.DataFrame(st.session_state.newly_generated_plan)
            st.dataframe(df, use_container_width=True)
        else:
            st.info("No new plan generated in this session. Click 'Generate and Save Plan'.")