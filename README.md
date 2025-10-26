
# Intrusive Revision

This is a multiple-choice flashcard app I have made for Android, with a simple design. It is intended for ease of use when you have a few minutes on your phone, and can be used offline.

The main feature of this project is the intrusive settings, where you can choose for it to popup on certain apps, for example social media, and give you a set number of questions to answer before letting you use the app.

It is targeted towards anyone who wants to integrate their learning into their daily life, and hopefully will be an effective tool!

The privacy policy can be accessed on this link: [https://layanjethwa.github.io/intrusive-revision/](https://layanjethwa.github.io/intrusive-revision/)

![Logo](https://github.com/LayanJethwa/intrusive-revision/blob/master/screenshots/logo-square.png)


## Installation

To install the app, simply head over to the releases tab, download the apk for the latest release, and open it on your Android device. A phone will work best (if the screen is wider than it is long, ie. a tablet, it may not work as intended).

The current release (v1.2) has all the basic features completed, and is ready for use! If any bugs are found, please report them in the issue tracker.

You can find a demo video here: [https://youtube.com/shorts/QtwILmPgwfY](https://youtube.com/shorts/QtwILmPgwfY)

To get started, copy and paste a URL from Quizlet (capped at 100 cards), or paste a Quizlet export (with "<" and ">" as separators)
    

## Features

- Fully offline storage and usage of flashcards
- Quizlet set scraper (capped at 100 cards for now)
  - Option to manually add Quizlet export ({name}:{term}<{def}>)
- Dynamic view for flashcard management
- Simple multiple-choice view to blitz through questions
- View local and global statistics for all sets, as well as per question
- Option to show questions based on previous statistics
- Intrusive settings
  - Starts on phone boot
  - You can select the number of questions, the penalty for incorrect answers, and the time interval between popups
  - You will be served popups when using specific apps at the specified time interval


## Roadmap

### High priority ###

- ~~Fix display bug upon set deletion~~ [DONE]

### Features ###
#### Statistics ####
- ~~Per session~~ [DONE]
- ~~Per set, all-time~~ [DONE]
- ~~Per question, all-time~~ [DONE]

#### Set management ####
- ~~Spaced retrieval~~ [DONE]
- Rename set
- Swap terms and definitions
- Allow for selection of multiple sets
    
#### Intrusive settings ####
- ~~Per app choices~~ [DONE]
- ~~Global choices~~ [DONE]
- ~~Number of questions to show~~ [DONE]
- ~~Frequency to show questions again~~ [DONE]
- ~~Penalty for incorrect answers~~ [DONE]
- ~~Counter for questions left to unlock close button~~ [DONE]

### Polishes ###
- ~~App icon~~ [DONE]
- ~~Colours for swipe bar~~ [DONE]
- ~~Animations and glow~~ [DONE]
- ~~Global/session stats switch colour~~ [DONE]
- ~~Checkbox colour~~ [DONE]
- ~~Sounds on right/wrong answers~~ [DONE]

### Eventual aims ###
- ~~Find a way to bypass 100 card limit~~ [MANUAL SOLUTION]


## Screenshots

![Home screen](https://github.com/LayanJethwa/intrusive-revision/blob/master/screenshots/home-screen.png)

![Flashcards screen](https://github.com/LayanJethwa/intrusive-revision/blob/master/screenshots/flashcards-screen.png)

![Incorrect answer](https://github.com/LayanJethwa/intrusive-revision/blob/master/screenshots/incorrect-answer.png)

![Adding flashcards](https://github.com/LayanJethwa/intrusive-revision/blob/master/screenshots/adding-flashcards.png)
