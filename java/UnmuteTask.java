public class UnmuteTask implements Runnable
{
	private String userid;
	
	public UnmuteTask(String userid)
	{
		this.userid = userid;
	}
	
	@Override
	public void run()
	{		
		VCJoinListener.unmute(userid);
		Main.jda.getTextChannelById(Main.LOG_CHANNEL).sendMessage(":loud_sound: "
				+ Main.jda.getUserById(userid).getAsTag() + " (" + userid + ") vc unmuted by " + Main.jda.getSelfUser().getAsTag()
				+ " for reason: Time's up!").queue();
		
		Main.scheduled_unmutes.remove(userid); //remove from pending unmutes list
	}
}
