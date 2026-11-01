package com.example.support;

public final class NeedsHumanException extends RuntimeException {

  private final String ticketId;
  private final Draft draft;
  private final String critique;

  public NeedsHumanException(String ticketId, Draft draft, String critique) {
    super("Needs human for ticket " + ticketId + ": " + critique);
    this.ticketId = ticketId;
    this.draft = draft;
    this.critique = critique;
  }

  public String ticketId() { return ticketId; }
  public Draft draft() { return draft; }
  public String critique() { return critique; }
}

/** Append-only evidence. Never overwrite a prior row. */
