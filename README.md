# Matrix Calculator & WiFi Signal Logger Android Projects

Welcome! This repository contains **two different Android projects**, separated into different Git branches.

| Branch Name        | Project Description                                                               |
|--------------------|-----------------------------------------------------------------------------------|
| `MatrixCalculator` | Android app to perform matrix operations using C++ native code and vector library |
| `WAPsScanner`| Android app to log WiFi signal strengths across multiple locations                |

---

## 📜 How to Access Each Project

1. Clone the repository:
   ```bash
   git clone https://github.com/IdhantArora7/MC_A3
   cd MC_A3
   ```

2. Checkout the branch you need:

   - Matrix Calculator:
     ```bash
     git checkout Q1
     ```

   - WiFi Signal Logger:
     ```bash
     git checkout Q2
     ```

Each branch has its own fully independent Android app with its own README.

---

## ✨ Project Summary

### Matrix Calculator (Branch: `Q1`)
- Input two matrices of any dimensions.
- Perform Add, Subtract, Multiply, and Element-wise Division.
- Matrix operations are implemented in **C++** (or similar) via **JNI**.

### WiFi Signal Logger (Branch: `Q2`)
- Scan and log WiFi RSSI (signal strength) values.
- Collect 100 samples per location.
- Distinguish and show data from **at least three different locations**.

---

## 🛠 Requirements
- Android Studio (latest version recommended)
- NDK + CMake installed
- Basic understanding of JNI for native code integration
- Permissions for WiFi scanning (ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE)

---
