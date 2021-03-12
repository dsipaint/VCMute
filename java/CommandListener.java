import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter
{	
	/*
	 * N.B:
	 * When manual guild mutes are detected by the API, there is no way to tell who muted, hence
	 * there is no way to differentiate between manual and command mutes/unmutes. As a result, all
	 * we can do is ignore manual mutes/unmutes, and discourage people from using them.
	 */
	public void onGuildMessageReceived(GuildMessageReceivedEvent e)
	{
		String msg = e.getMessage().getContentRaw();
		String[] args = msg.split(" ");
		
		
		if(isStaff(e.getMember()))
		{
			//^vcmute
			if(args[0].equalsIgnoreCase(Main.PREFIX + "vcmute"))
			{
				if(args.length == 1)
					return;
				
				
				String id;
				if(args[1].matches("\\d{18}")) //could be an id
					id = args[1];
				else if(args[1].matches("<@!\\d{18}>")) //mentioned member
					id = e.getMessage().getMentionedMembers().get(0).getId(); //get id of that member
				else
				{
					e.getChannel().sendMessage("Invalid member: " + args[1]).queue();
					return;
				}
				
				Member mutedperson = e.getGuild().getMemberById(id);
				
				//give priority to the new mute, and remove any unmute requests that were queued
				UnmuteTask task = Main.scheduled_unmutes.get(id);
				if(task != null)
					Main.scheduler.remove(task);
				
				//don't need to wait for them to be muted or unmuted any more
				VCJoinListener.pending_mutes.remove(id);
				VCJoinListener.pending_unmutes.remove(id);
				
				VCJoinListener.mute(id); //mute them
				
				String reason = "";
				if(args.length > 2)
				{
					for(int i = 2; i < args.length; i++)
						reason += args[i] + " ";
					
					reason = reason.substring(0, reason.length()); //remove space at end of string
				}
				
				e.getChannel().sendMessage(":mute: " + e.getGuild().getMemberById(id).getUser().getAsTag() 
						+ " was vc muted" + (reason.isEmpty() ? "" : " for reason: " + reason)).queue();
				
				e.getGuild().getTextChannelById(Main.LOG_CHANNEL).sendMessage(":mute: "  //emote and timestamp
						+ mutedperson.getUser().getAsTag() + " (" + mutedperson.getId() + ") vc muted by " + e.getAuthor().getAsTag() //who muted who
						+ (reason.isEmpty() ? "" : " for reason: " + reason)).queue(); //reason if provided
				
				mutedperson.getUser().openPrivateChannel().complete().sendMessage("You have been vc-muted in the Wilbur Soot Discord server"
						+ (reason.isEmpty() ? "" : " for reason: " + reason)
						+ ". If you would like to appeal this punishment, DM the Modmail bot and a member of staff will be in touch.")
				.queue();
				
				return;
			}
			
			//^vctempmute
			if(args[0].equalsIgnoreCase(Main.PREFIX + "vctempmute"))
			{
				if(args.length == 1)
					return;
				
				
				String id;
				if(args[1].matches("\\d{18}")) //could be an id
					id = args[1];
				else if(args[1].matches("<@!\\d{18}>")) //mentioned member
					id = e.getMessage().getMentionedMembers().get(0).getId(); //get id of that member
				else
				{
					e.getChannel().sendMessage("Invalid member: " + args[1]).queue();
					return;
				}
								
				long mutetime = stringToMillis(args[2]);
				if(mutetime < 0)
				{
					e.getChannel().sendMessage("Invalid mute time: " + args[2]).queue();
					return;
				}
				
				//give priority to the new mute, and remove any unmute requests that were queued
				UnmuteTask task = Main.scheduled_unmutes.get(id);
				if(task != null)
					Main.scheduler.remove(task);
				
				//don't need to wait for them to be muted or unmuted any more, for old requests
				VCJoinListener.pending_mutes.remove(id);
				VCJoinListener.pending_unmutes.remove(id);
				
				Member mutedperson = e.getGuild().getMemberById(id);
				
				VCJoinListener.mute(id);
				
				UnmuteTask unmutetask = new UnmuteTask(id);
				Main.scheduler.schedule(unmutetask, mutetime, TimeUnit.MILLISECONDS); //schedule the unmute
				Main.scheduled_unmutes.put(id, unmutetask); //store in hashmap in case of early unmute
				
				String reason = "";
				if(args.length > 3)
				{
					for(int i = 3; i < args.length; i++)
						reason += args[i] + " ";
					
					reason = reason.substring(0, reason.length()); //remove space at end of string
				}
				
				e.getChannel().sendMessage(":mute: " + e.getGuild().getMemberById(id).getUser().getAsTag() 
						+ " was vc muted for " + args[2]
						+ (reason.isEmpty() ? "" : " for reason: " + reason)).queue();
				
				e.getGuild().getTextChannelById(Main.LOG_CHANNEL).sendMessage(":mute: "  //emote and timestamp
						+ mutedperson.getUser().getAsTag() + " (" + mutedperson.getId() + ") vc muted by " + e.getAuthor().getAsTag() //who muted who
						+ " for " + args[2] //length of time
						+ (reason.isEmpty() ? "" : " for reason: " + reason)).queue(); //reason if provided
				
				mutedperson.getUser().openPrivateChannel().complete().sendMessage("You have been vc-muted in the Wilbur Soot Discord server"
						+ (reason.isEmpty() ? "" : " for reason: " + reason)
						+ ", for " + args[2]
						+ ". If you would like to appeal this punishment, DM the Modmail bot and a member of staff will be in touch.")
				.queue();
				
				return;
			}
			
			if(args[0].equalsIgnoreCase(Main.PREFIX + "vcunmute"))
			{
				if(args.length == 1)
					return;
				
				
				String id;
				if(args[1].matches("\\d{18}")) //could be an id
					id = args[1];
				else if(args[1].matches("<@!\\d{18}>")) //mentioned member
					id = e.getMessage().getMentionedMembers().get(0).getId(); //get id of that member
				else
				{
					e.getChannel().sendMessage("Invalid member: " + args[1]).queue();
					return;
				}
				
				Member mutedperson = e.getGuild().getMemberById(id);
				
				//check if this is an early unmute
				UnmuteTask scheduled_unmute = Main.scheduled_unmutes.get(id);
				if(scheduled_unmute != null) //if there is a scheduled unmute
					Main.scheduler.remove(scheduled_unmute); //remove it from the queue
				
				VCJoinListener.unmute(id);
				
				String reason = "";
				if(args.length > 2)
				{
					for(int i = 2; i < args.length; i++)
						reason += args[i] + " ";
					
					reason = reason.substring(0, reason.length()); //remove space at end of string
				}
				
				e.getChannel().sendMessage(":loud_sound: " + e.getGuild().getMemberById(id).getUser().getAsTag() 
						+ " was vc unmuted" + (reason.isEmpty() ? "" : " for reason: " + reason)).queue();
				
				e.getGuild().getTextChannelById(Main.LOG_CHANNEL).sendMessage(":loud_sound: " 
						+ mutedperson.getUser().getAsTag() + " (" + mutedperson.getId() + ") vc unmuted by " + e.getAuthor().getAsTag()
						+ (reason.isEmpty() ? "" : " for reason: " + reason)).queue();
				
				return;
			}
			
			//^disable vcmute
			if(args[0].equalsIgnoreCase(Main.PREFIX + "disable") && args[1].equalsIgnoreCase("vcmute"))
			{
				e.getChannel().sendMessage("*disabling vc mutes...*").complete();
				e.getJDA().shutdown();
				System.exit(0);
			}
		}
	}
	
	//return true if a member has discord mod, admin or is owner
			public static boolean isStaff(Member m)
			{
				try
				{
					//if owner
					if(m.isOwner())
						return true;
				}
				catch(NullPointerException e)
				{
					//no error message reee its pissing me off
				}
				
				//if admin
				if(m.hasPermission(Permission.ADMINISTRATOR))
					return true;
				
				for(Role r : m.getRoles())
				{
					if(r.getId().equals("565626094917648386")) //wilbur discord mod
						return true;
				}
				
				return false;
			}
			
		public static long stringToMillis(String time)
		{
			//a number plus s,m,h or d
			if(time.matches("\\d+[smhd]"))
			{
				long durationmillis = Long.parseLong(time.split("[smhd]")[0]);
				
				//for each letter
				switch(time.substring(time.split("[smhd]")[0].length()))
				{
					case "s":
						return durationmillis * 1000; 
						
					case "m":
						return durationmillis * 1000 * 60; 
						
					case "h":
						return durationmillis * 1000 * 3600; 
						
					case "d":
						return durationmillis * 1000 * 86400;
					
					default:
						return -1;
				}
			}
			
			return -1;
		}
}
