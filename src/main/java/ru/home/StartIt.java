/**
 * Echo-сервер
 * 
 * Данное приложение представляет собой echo-сервер,
 * работающий по стандарту RFC #862 ("The Echo
 * Protocol"). Сервер дополнен небольшим набором
 * команд, которые он выполняет по приказу клиента.
 * Клиент может подключаться к серверу через Telnet.
 * Приложение можно запускать с параметром. В качестве
 * параметра выступает TCP-порт, который сервер будет
 * прослушивать и принимать с него на вход данные.
 * 
 * @author Ефремов А. В., 18.10.2024
 */

package ru.home;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class StartIt {

  private static final int HTTP_PORT = 6666; // TCP port

  private static class ShoutyHandler extends ChannelInboundHandlerAdapter {

    private StringBuilder myCompleteMessage = new StringBuilder();
    private boolean isCompleteMessage = false;

    private void sendServerResponse(ChannelHandlerContext ctx, String msg, Charset chrst) {
      ctx.writeAndFlush(Unpooled.copiedBuffer(msg + "\r\n", chrst));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      final String osName = System.getProperty("os.name");
      boolean isRunning = true;
      try {
        Charset utf8 = CharsetUtil.UTF_8;

        String in = ((ByteBuf) msg).toString(utf8);
        char sym = in.charAt(in.length() - 1);
        if ((sym == (char) 10) || (sym == (char) 13)) { // the end-of-line characters
          isCompleteMessage = true;
        }
        myCompleteMessage.append(in.replaceAll("\n", "").replaceAll("\r", ""));

        if (isCompleteMessage) {
          printMessage("Данные от клиента: " + ((myCompleteMessage.toString().isEmpty()) ? "пустая строка" : String.valueOf((char) 34) + myCompleteMessage.toString().trim() + String.valueOf((char) 34)));
          String out = "HTTP/1.0 200 OK";
          sendServerResponse(ctx, out, utf8);
          printMessage("Ответ сервера: " + String.valueOf((char) 34) + out + String.valueOf((char) 34));
          final String clientCommand = "От клиента: ";
          switch (myCompleteMessage.toString().toLowerCase()) { // специальные команды от клиента для сервера
            case "exit": case "quit": case "stop": // команда завершения работы
              ctx.close();
              printMessage(clientCommand + "получена команда остановки сервера.");
              isRunning = false;
              break;
            case "time": case "date": // дата и время
              sendServerResponse(ctx, getCurrentTime(), utf8);
              printMessage(clientCommand + "запрос текущего времени.");
              break;
            case "version": case "ver": case "uname": // версия операционной системы
              sendServerResponse(ctx, osName + " " + System.getProperty("sun.arch.data.model") + ", " + System.getProperty("os.version"), utf8);
              printMessage(clientCommand + "версия операционной системы.");
              break;
            case "ps": case "process": case "processes": // список запущенных процессов в системе
              String psCmd;
              if (osName.toLowerCase().startsWith("windows")) {
                psCmd = System.getenv("windir") + "\\system32\\" + "tasklist.exe";
              } else {
                if ((osName.toLowerCase().startsWith("linux")) || (osName.toLowerCase().startsWith("mac"))) {
                  psCmd = "ps -e";
                } else {
                  sendServerResponse(ctx, "Sorry! An unknown operating system.", utf8);
                  psCmd = null;
                }
              }
              if (psCmd != null) {
                ArrayList<String> listOfLines = new ArrayList<String>();
                BufferedReader bufReader = null;
                try {
                  bufReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(psCmd).getInputStream()));
                  String line = bufReader.readLine();
                  while (line != null) {
                    listOfLines.add(line);
                    line = bufReader.readLine();
                  }
                }
                catch (IOException e) {}
                finally {
                  if (bufReader != null) {
                    try {
                      bufReader.close();
                    }
                    catch (IOException e) {}
                  }
                }
                String[] stringArray = listOfLines.toArray(new String[listOfLines.size()]); // dumping into a normal array
                if (stringArray.length > 0) {
                  for (String processItem : stringArray) {
                    sendServerResponse(ctx, processItem, utf8);
                  }
                  sendServerResponse(ctx, "Total: " + String.valueOf(stringArray.length) + ".", utf8);
                } else {
                  sendServerResponse(ctx, "No processes have been found.", utf8);
                }
              }
              printMessage(clientCommand + "список запущенных процессов в системе.");
              break;
            case "ip": // идентификация IP-адреса клиента на сервере
              sendServerResponse(ctx, ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress(), utf8);
              printMessage(clientCommand + "идентификация IP-адреса клиента на сервере.");
              break;
            case "help": // список команд
              sendServerResponse(ctx, "exit, quit, stop, time, date, version, ver, uname, ps, process, processes, ip, help", utf8);
              printMessage(clientCommand + "список команд.");
              break;
          }
        }
      }
      finally {
        ReferenceCountUtil.release(msg);
      }
      if (isRunning) {
        if (isCompleteMessage) {
          myCompleteMessage = new StringBuilder();
          isCompleteMessage = false;

          printMessage("Ожидание получения потока данных от клиента...");
        }
      } else {
        printMessage("Остановка сервера.");
        System.out.println("Работа программы завершена.");
        System.out.println(getCurrentTime());
        System.exit(0);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      cause.printStackTrace();
      ctx.close();
    }

  }

  @SuppressWarnings("rawtypes")
  private static class Init extends ChannelInitializer {

    @Override
    protected void initChannel(Channel ch) throws Exception {
      ch.pipeline().addLast(new ShoutyHandler());
    }

  }

  private static String getCurrentTime() {
    return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
  }

  private static void printMessage(String msg) {
    System.out.println("[" + getCurrentTime() + "] >> " + msg.trim());
  }

  private static int getTCPPort(String[] args) {
    int result = HTTP_PORT;
    if (args.length > 0) {
      try {
        result = Integer.parseInt(args[0]);
      }
      catch (NumberFormatException e) {}
    }
    if ((result < 1) || (result > 65535)) {
      result = HTTP_PORT;
    }
    return result;
  }

  public static void main(String[] args) throws InterruptedException {
    int port = getTCPPort(args);
    printMessage("Запуск сервера...");
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      printMessage("Сервер готов принимать соединения от клиента.");
      new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(new Init())
      .bind(port)
      .sync().channel().closeFuture().sync();
    }
    finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

}
