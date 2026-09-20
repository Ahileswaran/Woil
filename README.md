# WOIL: Workers' Opportunity and Inclusive Labour 🛡️📱⌚

> **A Mobile and Wearable Safety and Job Platform for Household Service Workers in Sri Lanka**  
> **BIT Final Year Project (ITE 3962)** | *Faculty of Information Technology, University of Moratuwa*

---

## 👨‍🎓 Project Information
- **Student Name:** Ahileswaran B
- **Registration No:** E2145015
- **Supervisor:** Dr. Tharindu Jayasuriya
- **Academic Year:** 2026
- **Repository:** [https://github.com/Ahileswaran/Woil](https://github.com/Ahileswaran/Woil)

---

## 📌 Project Overview
The informal domestic labour market represents ~60% of employment in Sri Lanka. Domestic workers (cleaners, caregivers, cooks, gardeners) encounter persistent challenges:
- **No structured job matching:** Dependence on volatile word-of-mouth recommendations.
- **Arbitrary wage setting:** Absence of fair minimum baselines or transparent calculation tools.
- **Identity trust barriers:** Inability to digitally verify credentials when entering private premises.
- **Workplace safety vulnerability:** Working alone in private residential environments without emergency safety escalation.

**WOIL** provides a comprehensive 4-tier solution combining a native Android mobile app, an ESP32 IoT wearable device (**Woil Guard**), a cloud backend with real-time database, and a centralized administrative **Core Control Center (CCC)**.

---

## 🏛️ Ecosystem Components

| Component | Description & Technology |
|---|---|
| 📱 **Mobile App** | Native Android (Java 11), Firebase BOM, Google ML Kit NIC OCR, Maps & Places SDK, WebRTC, Multi-tier Literacy UI, BLE Client. |
| ⌚ **IoT Wearable** | ESP32 microcontroller firmware, MPU6050 6-axis IMU, ST7735 TFT display, TinyML TensorFlow Lite fall-detection model, BLE GATT server, hardware panic SOS. |
| 🖥️ **CCC Dashboard & Backend** | Node.js Express backend and React/Vite web dashboard for dispute arbitration, NIC manual review queue, and emergency dispatch. |


---

## ✨ Core Innovations & Features

1. **Dual-Role Switching (Worker & Client):** Single-account toggle between household employer and worker modes.
2. **Multi-Literacy UI & Voice Guidance:** 3 interface modes (High, Medium, Low) with integrated Text-To-Speech (TTS) for low-literacy users.
3. **On-Device Sri Lankan NIC Verification:** Real-time ML Kit OCR engine validating both old (9-digit + V/X) and new (12-digit) NIC cards with automated checksum and gender/DOB verification.
4. **Geo-Location Worker Matching & Wage Calculator:** Interactive Google Maps location picker with proximity filtering and standardized baseline wage estimation.
5. **Woil Guard Wearable Emergency Safety:** Bluetooth Low Energy (BLE) link to an ESP32 wearable equipped with an SOS panic button and TinyML motion classification.
6. **Skill Showcase Portfolio:** Video recording and streaming powered by AndroidX Media3 ExoPlayer for verified proof of craftsmanship.

---


## 📜 Academic Integrity & License
This project was designed and implemented as part of the Bachelor of Information Technology (External) Degree at the University of Moratuwa. All rights reserved.
