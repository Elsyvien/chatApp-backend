/*
package com.maxstaneker.chatapp.chatappbackend;

import jakarta.servlet.ServletException;
import model.Message;
import model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * ChatServlet handles chat messages sent by users.
 * It allows users to send messages and retrieve the chat history.
 */
/*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

import java.util.ArrayList;

@WebServlet(name = "chatServlet", value = "/chat")
public class ChatServlet extends HttpServlet {

    private static final List<Message> messages = new ArrayList<>(); // Static list to hold chat messages

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) // Handles incoming chat messages
            throws ServletException, IOException {
        String sender = request.getParameter("sender");
        String content = request.getParameter("content");

        if (sender != null && content != null && !sender.trim().isEmpty() && !content.trim().isEmpty()) {
            Message message = new Message(sender, content, System.currentTimeMillis());
            messages.add(message);
            System.out.println("Message received: " + message.getContent() + " from " + message.getSender());
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            System.out.println("No sender or content received");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) // Fetches chat messages
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        System.out.println("Fetching chat messages...");

        out.println("<html><body>");
        out.println("<h2>Chat Messages</h2>");
        for (Message m : messages) {
            System.out.println(m);
            out.printf("<p><strong>%s:</strong> %s</p>", m.getSender(), m.getContent());
        }
        out.println("</body></html>");
    }
}
*/