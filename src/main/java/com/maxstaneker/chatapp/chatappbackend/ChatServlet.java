package com.maxstaneker.chatapp.chatappbackend;

import model.Message;
import model.User;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "chatServlet", value = "/chat")
public class ChatServlet {
    
}
