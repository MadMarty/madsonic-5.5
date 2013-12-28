package github.madmarty.madsonic.service.parser;

import android.content.Context;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.ChatMessage;
import github.madmarty.madsonic.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Joshua Bahnsen
 */
public class ChatMessageParser extends AbstractParser {

	public ChatMessageParser(Context context) {
        super(context);
    }

    public List<ChatMessage> parse(Reader reader, ProgressListener progressListener) throws Exception {
        updateProgress(progressListener, R.string.parser_reading);
        init(reader);
        List<ChatMessage> result = new ArrayList<ChatMessage>();
        int eventType;
        
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("chatMessage".equals(name)) {
                	ChatMessage chatMessage = new ChatMessage();
                	chatMessage.setUsername(get("username"));
                    chatMessage.setTime(getLong("time"));
                    chatMessage.setMessage(get("message"));
                    result.add(chatMessage);
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        updateProgress(progressListener, R.string.parser_reading_done);
        
        return result;
    }
}
