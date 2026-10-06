# AlgoPrepIdeas
Ideas on a new application that steals heavily from the design of ChatStory

I am writing this file in ChatStory so that Claude Code and Codex have access to the ChatStory code when deciding how to design AlgoPrep. Both Claude and Codex will create their own design documents here, and then those design documents will be used to create AlgoPrep in its own independent project

## Initial Idea

AlgoPrep is an application that will help its users prepare for interview coding problems - one example of this kind of problem is from leetcode.

Like ChatStory, AlgoPrep will have web browser type elements
Like ChatStory, AlgoPrep would have multiple windows. The leftmost window will be a display window for the app, typically showing the problem being worked on
Like ChatStory, the middle window will show ChatGPT
The right window will be for user interface and settings and such
At this time, there will be no bottom window.

### Basic Flow:
- User selects a problem in the right window.
- The problem displays in the left window.
- The user presses a INSTRUCTIONS button in the right window
- AlgoPrep reads the selected initial instructions file, and sends it to the ChatGPT window (much like how ChatStory sends the session controller instructions)
- The user uses the middle window to communicate with ChatGPT, and ChatGPT answers questions

### Behind the scenes:
- I will work with ChatGPT to construct the instructions file, but basically it will instruct ChatGPT to answer questions asked by the user but NOT give the user more than is asked. The goal of this is for the user to use ChatGPT to get just enough hints from ChatGPT so that the user can solve the programming problem by themselves.
- In another project, I have already organized a number of leetcode and other programming problems. they have the format of:
  - 0001_two-sum_problem.md - A description of the problem, in markdown
  - 0001_two-sum_notes.md - Some additional notes/hints
  - 0001_two-sum_solution.java - A solution I've written, if I've solved it
  - 0001_two-sum_testcases.md - Sample test cases
- We have not created a HOME directory yet. The user of AlgoPrep will put filename_AlgoPrepNotes.md files in that directory though

### RIGHT WINDOW
- User will have multiple tabs, starting with MAIN
- User will be able to select an Instructions file (and send it with the button)
- User will be able to select the PROBLEMS directory (where the leetcode questions are kept)
- User will be able to select the HOME directory (a directory on the user's machine where files (notes) can be saved or retrieved)
- User will have an UPLOAD button to upload the Problem files to ChatGPT (like ChatStory's method of sending context files) - but user will not select individual files. AlgoPrep will know which files to send.
- User will have a Problems tab, which will list the problems available in the PROBLEMS directory (just the problem number and name, not the _problem _notes etc files).
  - When the user double clicks a problem, that problem is put into the Display window
- User will have a Settings tab like ChatStory, so that the user can select light/dark mode and also which directories have the files. This will also include the staging directory, like ChatStory

### Display (left window)
- When the user selects a problem, there will be multiple tabs in this window. The first tab is for the problem (filename_problem.md file). The second tab is for any notes that came with the problem (filename_notes.md file). The third tab is for any notes that exist for the problem in the HOME directory that the user saved. For now, no other files will be used
