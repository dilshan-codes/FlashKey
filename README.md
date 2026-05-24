<h1>⚡ FlashKey 📱</h1>
<p>A lightweight, fast Android keyboard with a satisfying color flash animation on every keystroke.</p>

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2026-orange?style=flat-square)

<br>

<div align="center">
  <img src="ss's/images/1.jpeg" width="22%"/>
  &nbsp;&nbsp;
  <img src="ss's/images/2.jpeg" width="22%"/>
  &nbsp;&nbsp;
  <img src="ss's/images/3.jpeg" width="22%"/>
  &nbsp;&nbsp;
  <img src="ss's/images/4.jpeg" width="22%"/>
</div>

<br>

https://github.com/user-attachments/assets/72b90af5-1c5a-49ec-8d10-3acffa6ef33b

<br>
<hr>

<h3>✨ What is FlashKey?</h3>
<p>FlashKey is a custom Android keyboard built for one goal - feel satisfying to type on. Every key press triggers an instant warm orange flash that smoothly fades back to gray, creating a beautiful cascading light effect as you type.</p>
<p>No bloat. No AI suggestions. No cloud sync. Just a clean, fast keyboard that looks great and stays out of your way.</p>

<br>
<hr>

<h3>🎬 The Animation</h3>
<p>The signature feature. When you press a key:</p>

```
Finger touches key
      ↓
Key instantly flashes warm orange (#FFAA44)
      ↓
Smoothly fades back to gray over 600ms
      ↓
Rounded corners preserved throughout
```

<p>Press multiple keys quickly and the fades cascade beautifully across the keyboard.</p>

<br>
<hr>

<h3>🔑 Features</h3>
<ul>
  <li><strong>Keyboard Layout</strong>
    <ul>
      <li>Number row — 1 2 3 4 5 6 7 8 9 0 @ always visible at the top</li>
      <li>Standard QWERTY layout with centered rows like professional keyboards</li>
      <li>Comma, Period in the bottom row for quick access</li>
      <li>SPACE bar spanning the center of the bottom row</li>
    </ul>
  </li>
  <br>
  <li><strong>Typing</strong>
    <ul>
      <li>Lowercase by default — clean and readable</li>
      <li>SHF key — toggles uppercase, turns green when active</li>
      <li>Hold DEL — hold the delete key to keep deleting continuously</li>
      <li>Smart ENTER — label changes automatically based on the app you are typing in (ENT, DONE, SEND, GO, 🔍)</li>
    </ul>
  </li>
  <br>
  <li><strong>Symbols — 4-state system</strong>
    <br><br>

| State | SHF | SYM | Keys show |
|-------|-----|-----|-----------|
| Letters lower | Gray | Gray | a b c ... |
| Letters upper | Green | Gray | A B C ... |
| Symbols set 1 | Gray | Blue | ! ? # $ % ... |
| Symbols set 2 | Green | Blue | € £ ¥ © ® ... |

  </li>
  <br>
  <li><strong>Design</strong>
    <ul>
      <li>Dark keyboard background (#1a1a1a)</li>
      <li>Rounded letter keys (#606060)</li>
      <li>Darker action keys (#383838)</li>
      <li>Square number keys (#484848)</li>
      <li>Consistent 8dp corner radius everywhere</li>
      <li>Dynamic key sizing — adapts to any screen width automatically</li>
    </ul>
  </li>
</ul>

<br>
<hr>

<h3>📱 Installation</h3>

<strong>Prerequisites</strong>
<ul>
  <li>Android Studio (latest)</li>
  <li>Android phone with API 26+ (Android 8.0 or higher)</li>
  <li>USB cable + USB Debugging enabled on your phone</li>
</ul>

<strong>Steps</strong>

<strong>1. Clone the repo</strong>

```bash
git clone https://github.com/dilshan-codes/FlashKey.git
cd FlashKey
```

<strong>2. Open in Android Studio</strong>
<p>File → Open → select the FlashKey folder</p>

<strong>3. Build and install</strong>
<p>Connect your phone via USB, select it in the device dropdown, click the green Run button.</p>

<strong>4. Enable the keyboard on your phone</strong>
<p>Settings → Additional Settings → Keyboard & Input Method → Manage Keyboards → enable FlashKey → set as default</p>

<br>
<hr>

<h3>🛠 Built With</h3>
<ul>
  <li><strong>Kotlin</strong> — all logic and animation</li>
  <li><strong>XML</strong> — keyboard layout and styling</li>
  <li><strong>InputMethodService</strong> — Android's built-in keyboard framework</li>
  <li><strong>ValueAnimator</strong> — smooth color fade animation</li>
  <li><strong>GradientDrawable</strong> — rounded corners that survive animation</li>
  <li><strong>Handler + Runnable</strong> — hold-to-delete repeat logic</li>
</ul>

<br>
<hr>

<h3>📁 Project Structure</h3>

```
app/src/main/
├── java/com/dilshan/flashkey/
│   ├── FlashKeyService.kt          ← the keyboard — all logic lives here
│   └── MainActivity.kt             ← blank launcher activity
├── res/
│   ├── layout/
│   │   └── keyboard_view.xml       ← keyboard UI layout
│   ├── drawable/
│   │   ├── key_background.xml      ← letter key shape
│   │   ├── key_background_number.xml ← number key shape
│   │   └── key_background_action.xml ← action key shape
│   ├── values/
│   │   └── themes.xml              ← key styles
│   └── xml/
│       └── method.xml              ← IME registration
└── AndroidManifest.xml             ← registers FlashKeyService
```

<br>
<hr>

<h3>⚙️ Customization</h3>
<p>All the values you would want to tweak are at the top of <code>FlashKeyService.kt</code>:</p>

```kotlin
// Colors
private val colorLetterKey = 0xFF606060.toInt()   // letter key background
private val colorActionKey = 0xFF383838.toInt()   // SHF, DEL, SYM, SPACE, ENT
private val colorNumberKey = 0xFF484848.toInt()   // number row keys
private val colorFlashStart = 0xFFFFAA44.toInt()  // flash color on press
private val colorShiftActive = 0xFF4CAF50.toInt() // SHF when uppercase
private val colorSymActive = 0xFF2196F3.toInt()   // SYM when symbols showing

// Animation
private val flashDuration = 600L   // milliseconds — lower = faster fade

// Corner radius
private val cornerRadiusDp = 8f        // letter and action keys
private val cornerRadiusNumberDp = 4f  // number row keys
```

<p>Key height is set in <code>onCreateInputView()</code>:</p>

```kotlin
val letterKeyHeight = (letterKeyWidth * 1.3f).toInt()
// change 1.3f to make keys taller or shorter
```

<br>
<hr>

<h3>👤 Author</h3>
<p><strong>dilshan-codes</strong><br>
GitHub: <a href="https://github.com/dilshan-codes">@dilshan-codes</a></p>

<br>
<hr>

<h3>📄 License</h3>
<p>This project is open source and available under the MIT License.</p>
