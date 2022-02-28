import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.LinkedList;
import java.util.Queue;

public class TrackScheduler extends AudioEventAdapter {

    private Queue<AudioTrack> queue;
    private MessageChannel channel;
    private AudioPlayer audioPlayer;

    public TrackScheduler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        queue = new LinkedList<>();
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            // Start next track
            play();
            if(channel != null) {
                Bot.creeps++;
                if((Bot.creeps > 0) && (Bot.creeps % 5 == 0)) {
                    channel.sendMessage("creep **milestone " + (Bot.creeps / 5) + "** reached.").queue();
                } else {
                    channel.sendMessage("creep again anyone? (" + Bot.creeps + ")").queue();
                }
            }
        }

        if(endReason == AudioTrackEndReason.FINISHED) {

        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
    }

    public void queue(AudioTrack track) {
        queue.add(track);
    }

    public void play() {
        if(queue.isEmpty())
            return;
        AudioTrack track = queue.remove();
        AudioTrack clone = track.makeClone();
        queue.add(clone);
        audioPlayer.playTrack(track);
    }

    public void restart() {
        if(!audioPlayer.isPaused()) {
            audioPlayer.getPlayingTrack().setPosition(0);
        }
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    public void debug() {
        System.out.println("next in queue: " + queue.peek().getInfo().title);
    }
}
