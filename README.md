# Job Tracker

A Java backend that connects to a user's Gmail inbox, reads unread messages, and uses an NLP pipeline to classify job-application emails — confirmations, rejections, follow-ups, and offers — so applicants can track their job search without manually sorting their inbox.

## Features

- **Email integration** — connects to Gmail over IMAP (Jakarta/Angus Mail), retrieves unread messages, and supports marking messages as read, deleting them, and sending test emails via the same client.
- **Persistent storage** — SQLite (via JDBC) stores user credentials and extracted data between runs.
- **NLP classification pipeline** — built on Apache OpenNLP:
  - Tokenization, POS tagging, and lemmatization of email content
  - Named Entity Recognition (person and location models) to pull company and contact names out of message text
  - An n-gram and vocabulary-based scoring system that classifies each message into one of four categories: **confirmation, rejection, follow-up, or job offer**
- **Unit tests** (JUnit 5) covering the database layer and the mail manager: connecting, counting, reading, marking as read, and deleting messages.

## Status

This is an active work in progress, developed on the `Backend` branch ahead of merging into `master`.

**Working:**
- Gmail IMAP connection and credential storage
- Unread email retrieval
- Named entity extraction (person / location)
- Message scoring logic (confirmation / rejection / follow-up / offer)

**In progress:**
- Wiring the scoring pipeline into the main email-processing flow (implemented, not yet called end-to-end)
- Persisting extracted company/location data to the database
- A graphical interface — currently CLI-only
- Moving stored credentials out of plaintext SQLite (e.g. OS keychain or encrypted storage)

## Tech stack

Java 21 · Maven · Jakarta Mail (Angus Mail) · SQLite (JDBC) · Apache OpenNLP · JUnit 5


## Roadmap

- [ ] Connect the scoring engine to the main email-processing loop
- [ ] Persist extracted company/location data
- [ ] Build a simple GUI to view tracked applications
- [ ] Replace plaintext credential storage with a secure alternative
