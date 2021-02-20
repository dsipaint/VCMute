import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Main
{
	static JDA jda;
	static final String PREFIX = "^",
			GUILD_ID = "565623426501443584",
			LOG_CHANNEL = "771463923449987104";
	
	static ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
	static HashMap<String, UnmuteTask> scheduled_unmutes = new HashMap<String, UnmuteTask>();
	
	public static void main(String[] args)
	{
		try
		{
			jda = JDABuilder.createDefault("")
					.enableIntents(GatewayIntent.GUILD_MEMBERS)
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.build();
		}
		catch (LoginException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			jda.awaitReady();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		jda.getGuildById(GUILD_ID).loadMembers();
		
		jda.addEventListener(new CommandListener());
		jda.addEventListener(new VCJoinListener());
	}
}
