import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.VoidDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import javax.sound.midi.Track;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter implements NativeKeyListener {

    public static int creeps = 0;

    public static final long BAD_MEME_COOLDOWN = 10 * 60 * 1000;
    private static final boolean PRINT_TIME = true;

    private final String[] leaveKeywords = {"leave", "stop", "go away", "get out of here", "i hate you"};
    private final String[] whoWroteKeywords = {"who is creep by", "who made creep", "who wrote creep"};
    private final String[] startOverKeywords = {"restart", "start over", "again"};

    private Set<String> matches;
    private List<String> keywords;

    private ShortcutListener shortcutListener;
    private MessageChannel activeChannel;

    private AudioPlayerManager APM;
    private AudioPlayer AP;
    private TrackScheduler trackScheduler;
    private AudioPlayerSendHandler APSH;

    private AudioTrack theCreep;
    private AudioTrack notCreep;

    private long lastBadMeme = 0;

    private List<String> whoWroteCreep;
    private List<Pair<String, String>> restartingCreep;

    public static void main(String[] args) throws LoginException {
        if (args.length == 0) {
            System.out.println("give me TOKEN!!!");
            System.exit(1);
        }

        //JDA builder
        Bot bot = new Bot();

        JDABuilder.create(args[0], GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(bot)
                .setActivity(Activity.listening("Creep"))
                .build();

        // ADD KEYPRESS LISTENER
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }


        GlobalScreen.addNativeKeyListener(bot.getShortcutListener());
    }

    public Bot() {
        //Load matchlist (message must match whole phrase)
        matches = new HashSet<>();
        InputStream is = Bot.class.getClassLoader().getResourceAsStream("matchlist.txt");
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .filter(s -> !s.startsWith("//"))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(matches::add);

        //Load keywords (message need only contain keyword)
        keywords = new ArrayList<>();
        InputStream kis = Bot.class.getClassLoader().getResourceAsStream("keywords.txt");

        new BufferedReader(new InputStreamReader(kis, StandardCharsets.UTF_8))
                .lines()
                .filter(s -> !s.startsWith("//"))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(keywords::add);

        //DEBUG but i kinda like it
        for(String str : keywords) {
            System.out.println(str);
        }

        shortcutListener = new ShortcutListener(getActiveChannel());
        activeChannel = null;

        //register lavaplayer stuff
        APM = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(APM);
        AP = APM.createPlayer();

        trackScheduler = new TrackScheduler(AP);
        AP.addListener(trackScheduler);

        APSH = new AudioPlayerSendHandler(AP);

        //load in track(s) (just 1. Just creep.)
        APM.loadItem("XFkzRNyygfk", new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                trackScheduler.queue(track);
                theCreep = track;
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    trackScheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                // Notify the user that we've got nothing
                System.out.println("couldn't find creep!!!!!!!!!");

            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                // Notify the user that everything exploded
            }
        });

        //load in another song
        APM.loadItem("YogCyIXNrMg", new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                notCreep = track;
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    trackScheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                // Notify the user that we've got nothing
                System.out.println("wher er is indian moonlight bass bost/?");

            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                // Notify the user that everything exploded
            }
        });

        // hardcoding is awesome
        // responses for who wrote creep
        whoWroteCreep = new ArrayList<>();
        whoWroteCreep.add("idk");
        whoWroteCreep.add("somebody");
        whoWroteCreep.add("rriadhohade");
        whoWroteCreep.add("Simple. Google it and fuck off. Now.");
        whoWroteCreep.add("Creep is the 2nd track off of Radiohead's freshman album, \"Pablo Honey\".");

        // responses for restarting creep
            restartingCreep = new ArrayList<>();
            restartingCreep.add(Pair.of("ah yes, good choice sir! Creep for the ", "th time."));
            restartingCreep.add(Pair.of("let's keep listening to creep! ", " times so far!"));
            restartingCreep.add(Pair.of("crepeing for ", " time.! very nice~~"));
            restartingCreep.add(Pair.of("start over creep anyone? (", ")"));
            restartingCreep.add(Pair.of("!CREEPNUM", ""));
    }



    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        String msgTxt = msg.getContentRaw().toLowerCase(Locale.ROOT);
        boolean msgMatches = false;
        String matched = "";

        System.out.println("TIME: " + System.currentTimeMillis());
        if(matches.contains(msgTxt)) { //matchlist match
            msgMatches = true;
            matched = msgTxt;

            System.out.println("matched on lyric");
        } else {
            for(String keyword : keywords) {
                if(msgTxt.contains(keyword)) {
                    msgMatches = true;
                    matched = keyword;

                    System.out.println("matched on keyword: " + keyword);
                }
            }
        }
        MessageChannel channel = event.getChannel();
        shortcutListener.setChannel(channel);
        trackScheduler.setChannel(channel);

        if(msgTxt.startsWith("go away creep")
                || msgTxt.startsWith("go away weirdo")
                || msgTxt.startsWith("leave creep")
                || msgTxt.startsWith("leave weirdo")) {
            // Gets the channel in which the bot is currently connected.
            AudioChannel connectedChannel = event.getGuild().getSelfMember().getVoiceState().getChannel();
            // Checks if the bot is connected to a voice channel.
            if(connectedChannel == null) {
                channel.sendMessage(";-; i'm not even creeping right now!").queue();
                return;
            }
            // Disconnect from the channel.
            event.getGuild().getAudioManager().closeAudioConnection();
            // Notify the user.
            channel.sendMessage("ok bye :(").queue();
        } else if(msgTxt.startsWith("who wrote creep")
                || msgTxt.startsWith("who is creep by")
                || msgTxt.startsWith("who made creep")) {

            Collections.shuffle(whoWroteCreep);

            channel.sendMessage(whoWroteCreep.get(0)).queue();

        } else if( msgTxt.startsWith("restart")
                || msgTxt.startsWith("start creep over")
                || msgTxt.startsWith("start over")){

            Collections.shuffle(restartingCreep);

            if(restartingCreep.get(0).getLeft().equals("!CREEPNUM")) {
                StringBuilder creepsStr = new StringBuilder();
                creepsStr.append("CREEP ".repeat(creeps));
                if(creeps == 0) {
                    channel.sendMessage("you've seriously listed 0 fucking times all the way through??? creep is a great song man. fuck you man.").queue();
                    return;
                }
                channel.sendMessage(creepsStr.toString()).queue();
                return;
            }

            channel.sendMessage(restartingCreep.get(0).getLeft() + creeps + restartingCreep.get(0).getRight()).queue();
            trackScheduler.restart();

        } else if(msgTxt.startsWith("skip")) {
            AP.stopTrack();
            trackScheduler.play();
            channel.sendMessage("playing creep again").queue();
            return;
        } else if(msgTxt.contains("stop creep") || msgTxt.contains("stop weirdo")) {
            channel.sendMessage("no").queue();
            return;
        } else if(msgTxt.startsWith("run mcafee antivirus")) {
            AP.stopTrack();
            while(!trackScheduler.getQueue().isEmpty()) {
                trackScheduler.getQueue().remove();
            }
            trackScheduler.getQueue().add(theCreep.makeClone());
            trackScheduler.play();

            channel.sendMessage("[**MCAFEE**] /--MALWARE REMOVED--/\n$499.99 must be pay in Google Play store gift cards. Send the codes in chat now.").queue();

            return;
        } else if(msgMatches || msgTxt.equals("free 123 punjabi movie")) {
            System.out.println("made it to matchLoop");
            GuildChannel guildChannel = event.getGuildChannel();

            if(event.getAuthor().isBot()) {
                return;
            }

            if(!event.getGuild().getSelfMember().hasPermission(guildChannel, Permission.VOICE_CONNECT)) {
                // The bot does not have permission to join any voice channel. Don't forget the .queue()!
                channel.sendMessage("No permissions to creep into your channel!").queue();
                return;
            }

            GuildVoiceState vs = event.getMember().getVoiceState();
            if(vs == null) {
                channel.sendMessage("yo'eredont even have a voice state...?>?? what the crap...").queue();
                return;
            }

            System.out.println("creepy member: " + event.getMember());

            VoiceChannel connectedChannel = (VoiceChannel) vs.getChannel();

            if(!vs.inAudioChannel()) {
                channel.sendMessage("You're not even in a VC dude... crigne").queue();
                return;
            }
            // Gets the audio manager
            AudioManager audioManager = event.getGuild().getAudioManager();
            if(audioManager.isConnected() && !msgTxt.equals("free 123 punjabi movie")) {
                channel.sendMessage("already creeping!1!!!").queue();
                return;
            }
            // Connects to the channel.
            audioManager.openAudioConnection(connectedChannel);
            audioManager.setSendingHandler(APSH);
            trackScheduler.debug();

            if(msgTxt.equals("free 123 punjabi movie")) {
                System.out.println("--bad meme called--");
                if((System.currentTimeMillis() >= (lastBadMeme + BAD_MEME_COOLDOWN)) || (lastBadMeme == 0) ) { //cooldown has passed
                    lastBadMeme = System.currentTimeMillis();
                } else {
                    channel.sendMessage("L + ratio").queue(); //cooldown active, can't use joke
                    return;
                }
                //shitposting
                channel.sendMessage("[**CRITICAL ERROR**] /--MALWARE DETECTED--/\ni am indian and let me tell you, being indian is actually a great thing. we have beautiful people and we have ugly people. just like any race. I for 1 am a great person, have great friends, and i take care of every one that is in my life and i am glad to do it, and for that i have people that love me around me. also, indian parents are one of the best parents you can have. sometimes they can be really strict, but they will take care of you no matter what, and they will buy you what ever it is that you need. as long as your not a selfish ****head. Also, we all have great jobs, lots of money and a family that we all love and take care off. suck my dick op suck my dick.").queue();

                AP.stopTrack();
                AP.playTrack(notCreep.makeClone());
                return;
            }

            trackScheduler.play();

            //say we've connected ... kinda
            if(matched.equals("creep")) { //hardcode for stupid replies
                channel.sendMessage("you're a weirdo.").queue();
            } else if(matched.equals("weirdo")) {
                channel.sendMessage("you're a creep.").queue();
            } else {
                channel.sendMessage("creepy!").queue(); //TODO: placeholder i think
            }

        }

    }

    public MessageChannel getActiveChannel() {
        return activeChannel;
    }

    public ShortcutListener getShortcutListener() {
        return shortcutListener;
    }

    private boolean validateMsg(String str, String[]... validsArrs) {
        String str1 = str;
        boolean[] includes = new boolean[validsArrs.length];
        int i = 0;
        for(String[] valids : validsArrs) {
            inner:
            for(String valid : valids) {
                if(str1.contains(valid)) {
                    str1 = str1.replace(valid, "");
                    includes[i] = true;
                    i++;
                    break inner;
                }
            }
        }
        for(boolean include : includes) {
            if(!include) {
                return false;
            }
        }
        return true;
    }
}
