import java.util.ArrayList;

import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class VCJoinListener extends ListenerAdapter
{
	static ArrayList<String> pending_unmutes = new ArrayList<String>(),
			pending_mutes = new ArrayList<String>();
	
	public void onGuildVoiceJoin(GuildVoiceJoinEvent e)
	{
		String userid = e.getMember().getId();
		
		if(pending_mutes.contains(userid))
		{	
			mute(userid);
			return;
		}
		
		if(pending_unmutes.contains(userid))
		{
			unmute(userid);
			return;
		}
	}
	
	public static boolean mute(String userid)
	{
		pending_mutes.remove(userid); //remove, we don't need to wait for this to happen any more
		pending_unmutes.remove(userid); //we can safely remove an unmute request if a mute is requested
		
		for(VoiceChannel vc : Main.jda.getGuildById(Main.GUILD_ID).getVoiceChannels())
		{
			if(vc.getMembers().contains(Main.jda.getGuildById(Main.GUILD_ID).getMemberById(userid)))
			{
				Main.jda.getGuildById(Main.GUILD_ID).getMemberById(userid).mute(true).queue();
				return true;
			}
		}
		
		pending_mutes.add(userid); //make sure we listen for them if we couldn't get them this time
		
		return false;
	}
	
	public static boolean unmute(String userid)
	{
		pending_unmutes.remove(userid); //remove, we don't need to wait for this to happen any more
		pending_mutes.remove(userid); //we can safely remove a mute request if an unmute is requested
		
		for(VoiceChannel vc : Main.jda.getGuildById(Main.GUILD_ID).getVoiceChannels())
		{
			if(vc.getMembers().contains(Main.jda.getGuildById(Main.GUILD_ID).getMemberById(userid)))
			{
				Main.jda.getGuildById(Main.GUILD_ID).getMemberById(userid).mute(false).queue();	
				return true;
			}
		}
		
		pending_unmutes.add(userid); //make sure we listen for them if we couldn't get them this time
		
		return false;
	}
}
