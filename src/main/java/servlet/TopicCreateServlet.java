package servlet;

import org.bson.ByteBuf;
import org.elasticsearch.action.percolate.PercolateResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Created by slgu1 on 11/7/15.
 */
public class TopicCreateServlet extends HttpServlet {
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(?<start>\\d*)-(?<end>\\d*)");
    private static final int BUFFER_LENGTH = 1024 * 16;
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String range = req.getHeader("range");
        Matcher matcher = RANGE_PATTERN.matcher(range);
        Path video = Paths.get("/Users/slgu1/Desktop/", "test.mp4");
        int len = (int) Files.size(video);
        int start = 0;
        int end = len - 1;

        if (matcher.matches()) {
            String startGroup = matcher.group("start");
            start = startGroup.isEmpty() ? start : Integer.valueOf(startGroup);
            start = start < 0 ? 0 : start;
            String endGroup = matcher.group("end");
            end = endGroup.isEmpty() ? end : Integer.valueOf(endGroup);
            end = end > len - 1 ? len - 1 : end;
        }

        int contentLength = end - start + 1;

        resp.reset();
        resp.setBufferSize(BUFFER_LENGTH);
        resp.setHeader("Content-Disposition", String.format("inline;filename=\"%s\"", "test.mp4"));
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setDateHeader("Last-Modified", Files.getLastModifiedTime(video).toMillis());
        resp.setDateHeader("Expires", System.currentTimeMillis() + EXPIRE_TIME);
        resp.setContentType(Files.probeContentType(video));
        resp.setHeader("Content-Range", String.format("bytes %s-%s/%s", start, end, len));
        resp.setHeader("Content-Length", String.format("%s", contentLength));
        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        int bytesRead;
        int bytesLeft = contentLength;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_LENGTH);
        SeekableByteChannel input = Files.newByteChannel(video, READ);
        OutputStream output = resp.getOutputStream();
        input.position(start);
        while ((bytesRead = input.read(buffer)) != -1 && bytesLeft > 0) {
            System.out.println(bytesRead);
            //buffer.clear();
            output.write(buffer.array(), 0, bytesLeft < bytesRead ? bytesLeft : bytesRead);
            bytesLeft -= bytesRead;
        }
    }
}