package modreq;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import modreq.Metrics.Graph;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class modreq extends JavaPlugin  {
	public YamlConfiguration Messages;
	public static modreq plugin;
	public TicketHandler tickets;
	public final Logger logger = Logger.getLogger("Minecraft");
	public ModReqCommandExecutor myExecutor;
	public File configFile;
	private File messages;
	private Metrics metrics;
	public String latestVersion;
	public String DownloadLink;
	
	

	
	@Override
	public void onEnable() {
		plugin = this;
		messages = new File(getDataFolder().getAbsolutePath()+ "/messages.yml");
		configFile = new File(getDataFolder().getAbsolutePath()+ "/config.yml");
		if(!configFile.exists()) {
			firstrun();
		}
		YamlConfiguration pluginYML = YamlConfiguration.loadConfiguration(this.getResource("plugin.yml"));
		if(!pluginYML.getString("config-version").equals(getConfig().getString("version"))) {
			logger.info("[ModReq] You plugin version does not match the config version. Please visit the bukkitdev page for more information");
		}
		loadMessages();
		
		myExecutor = new ModReqCommandExecutor(this);
		getCommand("modreq").setExecutor(myExecutor);
		getCommand("check").setExecutor(myExecutor);
		getCommand("tp-id").setExecutor(myExecutor);
		getCommand("claim").setExecutor(myExecutor);
		getCommand("re-open").setExecutor(myExecutor);
		getCommand("status").setExecutor(myExecutor);
		getCommand("done").setExecutor(myExecutor);
		getCommand("mods").setExecutor(myExecutor);
		getCommand("modhelp").setExecutor(myExecutor);
		getCommand("updatemodreq").setExecutor(myExecutor);
		
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new ModReqListener(this), this);
		
		PluginDescriptionFile pdfFile = this.getDescription();
		tickets = new TicketHandler();
		startVersionChecker();
		if(this.getConfig().getBoolean("metrics")) {	
			try {
				metrics = new Metrics(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
			startNotify();
			startGraphs();
			logger.info("[ModReq] Using metrics");
		}
		this.logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled.");
	}
	
	private void startVersionChecker() {
		Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new VersionChecker(this), 60, 36000);		
	}
	private void startGraphs() {
		metrics.start();
		try {//test chart
		    Metrics metrics = new Metrics(plugin);

		    // Construct a graph, which can be immediately used and considered as valid
		    Graph graph = metrics.createGraph("Tickets");

		    // total
		    graph.addPlotter(new Metrics.Plotter("Open") {

		            @Override
		            public int getValue() {
		                    return tickets.getTicketAmount(Status.OPEN); // Number of players who used a diamond sword
		            }

		    });
		    graph.addPlotter(new Metrics.Plotter("Claimed") {

	            @Override
	            public int getValue() {
	                    return tickets.getTicketAmount(Status.CLAIMED); // Number of players who used a diamond sword
	            }

		    });
		    graph.addPlotter(new Metrics.Plotter("Closed") {

	            @Override
	            public int getValue() {
	                    return tickets.getTicketAmount(Status.CLOSED); // Number of players who used a diamond sword
	            }

	    });

		    metrics.start();
		} catch (IOException e) {
		    
		}
		
	}
	private boolean checkTranslate() {
		if(!messages.exists()) {
			return false;
		}
		else {
			return true;
		}
	}
	private void loadMessages() {
		logger.info("[ModReq] Looking for messages.yml");
		if (checkTranslate()) {
			logger.info("[ModReq] messages.yml found. Trying to load messages.yml");
			Messages = YamlConfiguration.loadConfiguration(messages);
			logger.info("[ModReq] messages.yml loaded!");
			return;
		}
		else {
			logger.info("[ModReq] messages.yml not found, using default messages");
			saveDefaultMessages();
			Messages = getDefaultMessages();
			return;		
		}
	}
	private void saveDefaultMessages() {
		plugin.saveResource("messages.yml", true);
		
	}
	public YamlConfiguration getDefaultMessages() {
		YamlConfiguration pluginYML = YamlConfiguration.loadConfiguration(this.getResource("messages.yml"));	
		return pluginYML;
	}
	private void startNotify() {
		if(this.getConfig().getBoolean("notify-on-time")){
			logger.info("[ModReq] Notifying on time enabled");
			long time = this.getConfig().getLong("time-period");
			time = time * 1200;
			Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
				
				   public void run() {
					   TicketHandler th = new TicketHandler();
					   int opentickets = th.getOpenTicketsAmount();
					   if(opentickets > 0) {
						   Player[] online = Bukkit.getOnlinePlayers();
						   for(int i=0; i<online.length;i++) {
							   if(online[i].hasPermission("modreq.check")) {
								   online[i].sendMessage(ChatColor.GOLD+"[ModReq]" + ChatColor.GREEN + Integer.toString(opentickets) + plugin.Messages.getString("notification", "open tickets are waiting for you"));
							   }
						   }
					   }
				   }
			}, 60L, time);
		}	
	}
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " is now disabled ");
	}
	private void firstrun() {//create the config.yml
		this.saveDefaultConfig();
	}

}