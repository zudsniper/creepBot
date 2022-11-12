import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Scanner;

public class ShortcutListener implements NativeKeyListener {

    private MessageChannel channel;

    private boolean shiftPressed = false;
    private boolean ctrlPressed = false;

    public ShortcutListener(MessageChannel messageChannel) {
        channel = messageChannel;
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        if(nativeEvent.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = true;
        }
        if(nativeEvent.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = true;
        }
        if(shiftPressed && ctrlPressed && nativeEvent.getKeyCode() == NativeKeyEvent.VC_T) {
            Scanner sin = new Scanner(System.in);
            System.out.println("CHATTING. Type 'quit' to quit. ");
            boolean quit = false;
            while(!quit) {
                System.out.print("> ");
                String message = null;

                message = sin.nextLine();

                if(message.equals("quit")) {
                    System.out.println("CHAT CANCELLED.");
                    quit = true;
                } else {
                    channel.sendMessage(message).queue();
                }


            }

        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        if(nativeEvent.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = false;
        }
        if(nativeEvent.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = false;
        }
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }
}
