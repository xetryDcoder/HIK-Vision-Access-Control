import requests

def fetch_access_control_events():
    url = "http://192.168.1.113/ISAPI/AccessControl/AcsEvent?format=json"
    payload = {
        "AcsEventCond": {
            "searchID": "1",
            "searchResultPosition": 0,
            "maxResults": 999999,
            "major": 0,
            "minor": 0,
            "startTime": "2024-06-07T00:00:00+05:45",
            "endTime": "2024-06-07T23:59:59+05:45",
            "eventAttribute": "attendance"
        }
    }
    headers = {
        "Content-Type": "application/json"
    }
    auth = requests.auth.HTTPDigestAuth("admin", "Ultimate@22")

    try:
        response = requests.post(url, json=payload, headers=headers, auth=auth)
        response.raise_for_status()  # Raise an exception for bad status codes
        data = response.json()
        return data
    except requests.exceptions.RequestException as e:
        print("Error fetching access control events:", e)
        return None

# Example usage:
access_control_events = fetch_access_control_events()
if access_control_events:
    print(access_control_events)
    # Process the events further as needed
else:
    print("Failed to fetch access control events.")
