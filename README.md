Mobile Malware Scanning, Advanced Threat Hunting, and Cyber Threat Intelligence
========
Smart phone application for Android devices for scanning files with hash databases plus YARA and Sigma as well as API integrations for open source threat hunting and intelligence.

TO BE UPDATED!

I am adding many features to this app and ClamAV is just one of them. I aim for this to be a swiss army knife for Android cybersecurity.

Steps used by the application:

1. The Android application connects to a server.
2. Application sends the files to the server and invokes the ClamAV process.
3. ClamAv performs the scan of the files using ClamScan.
4. Once Scan is complete it generates a report on the scanned file.
5. Server then sends the scan report back to the user (Application).
