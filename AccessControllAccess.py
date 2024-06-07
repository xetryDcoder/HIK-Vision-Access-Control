import sys
import requests
from datetime import datetime

def fetch_access_control_events(ip_address):
    url = f"http://{ip_address}/ISAPI/AccessControl/AcsEvent?format=json"

    today = datetime.now()
    start_time = today.strftime("%Y-%m-%dT00:00:00+05:45")
    end_time = today.replace(hour=23, minute=59, second=59).strftime("%Y-%m-%dT%H:%M:%S+05:45")

    payload = {
        "AcsEventCond": {
            "searchID": "1",
            "searchResultPosition": 0,
            "maxResults": 999999,
            "major": 0,
            "minor": 0,
            "startTime": start_time,
            "endTime": end_time,
            "eventAttribute": "attendance"
        }
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    auth = requests.auth.HTTPDigestAuth("admin", "Ultimate@22")

    try:
        response = requests.post(url, json=payload, headers=headers, auth=auth)
        response.raise_for_status()
        data = response.json()
        return data
    except requests.exceptions.RequestException as e:
        print("Error fetching access control events:", e)
        return None

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python AccessControllAccess.py <IP>")
        sys.exit(1)
    
    ip_address = sys.argv[1]
    access_control_events = fetch_access_control_events(ip_address)
    if access_control_events:
        print(access_control_events)
        sys.exit(0)  # Return 0 for success
    else:
        sys.exit(1)  # Return non-zero for failure
