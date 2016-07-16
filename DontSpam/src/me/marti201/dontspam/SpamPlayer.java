package me.marti201.dontspam;

public class SpamPlayer {

	int repeatsChat, repeatsCommand = 0;
	String lastChat, lastCommand;

	// Returns true if the message should be cancelled
	public boolean processChat(String message) {
		if (DontSpam.allowedRepeatsChat <= 0)
			return false; // Return false if disabled
		
		if (lastChat != null && message.contains(lastChat))
			repeatsChat++;
		else {
			repeatsChat = 0;
			lastChat = message;
		}

		return repeatsChat >= DontSpam.allowedRepeatsChat;
	}

	// Returns true if the command should be cancelled
	public boolean processCommand(String cmd) {
		if (DontSpam.allowedRepeatsCommands <= 0)
			return false;// Return false if disabled
		
		if (lastCommand != null && lastCommand.equals(cmd))
			repeatsCommand++;
		else {
			repeatsCommand = 0;
			lastCommand = cmd;
		}

		return repeatsCommand >= DontSpam.allowedRepeatsCommands;
	}

}
