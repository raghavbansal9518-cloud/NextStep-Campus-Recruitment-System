# Dhruv — Presentation Layer, CLI Gateway & Demo Systems

## 1. Aapka Project Mein Role:
Aapne **NextStep** ka Complete Presentation Layer, Interactive Terminal Navigation, Visual Styling, and Live Demonstration System design kiya hai.

---

## 2. Aapki Assigned Files:
1. `Main.java` — Master application controller, gateway loop, student & officer workflows, input loops.
2. `DemoService.java` — Demonstration scenarios, pre-configured test profiles (Eligible, Borderline, Ineligible students).
3. `ConsoleTheme.java` — ASCII UI styling, colors (ANSI escapes), cards, dividers, headers.
4. `PresentationConfig.java` — Dynamic presentation visibility flags and runtime configuration toggles.

---

## 3. Sir ko 1-Minute Mein Kya Bolna Hai:
> *"Sir, maine application ka Presentation Layer aur CLI User Experience design kiya hai. Terminal par clean navigation ke liye humne universal ASCII formatting aur ANSI color coding use ki hai taaki har Windows ya Linux terminal par UI consistent dikhe. Main.java mein stateful workflow loops hain jo invalid inputs par crash hone ke bajaye graceful user warnings dete hain. Sath hi viva demonstration ke liye humne 1-Click Auto-Fill (Shortcut 'A') implement kiya hai taaki live presentation ke dauran typing error na ho aur demo seamless chale."*

---

## 4. Sir Kya Questions Puch Sakte Hain? (Answers ke sath):

* **Q1: Main.java itna bada kyun hai? Iska architecture kya hai?**
  * **Ans:** `Main.java` presentation controller hai. Isme Student Portal, Placement Officer Portal aur Campus Gateway ke workflows hain. Har page ke liye dedicated render methods hain aur ye user inputs ko sanitize karke respective Services ko delegate karta hai.

* **Q2: Password enter karte waqt terminal par dikhta kyun nahi?**
  * **Ans:** `System.console().readPassword()` use kiya gaya hai taaki password screen par echo na ho (shoulder-surfing prevention).

* **Q3: Demo Mode ka kya purpose hai?**
  * **Ans:** DemoService realistic scenarios provide karta hai: Profile A (Fully Eligible), Profile B (Low CGPA / Ineligible), Profile C (Backlog failure), taaki evaluation committee ko system ke saare edge cases 10 seconds mein live verify karwaye ja sakein.
