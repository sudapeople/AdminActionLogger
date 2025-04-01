package com.suda.creativeitemlogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CreativeLoggerCommand implements CommandExecutor, TabCompleter {

    private final CreativeItemLogger plugin;

    public CreativeLoggerCommand(CreativeItemLogger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("creativeitemlogger.admin")) {
            sender.sendMessage(Component.text("이 명령어를 사용할 권한이 없습니다.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle":
                boolean newState = !plugin.isLoggingEnabled();
                plugin.setLoggingEnabled(newState);
                
                if (newState) {
                    sender.sendMessage(
                        Component.text("관리자 행동 로깅이 ")
                            .color(NamedTextColor.GREEN)
                            .append(Component.text("활성화").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                            .append(Component.text("되었습니다.").color(NamedTextColor.GREEN))
                    );
                } else {
                    sender.sendMessage(
                        Component.text("관리자 행동 로깅이 ")
                            .color(NamedTextColor.GREEN)
                            .append(Component.text("비활성화").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                            .append(Component.text("되었습니다.").color(NamedTextColor.GREEN))
                    );
                }
                break;
                
            case "reload":
                plugin.loadConfig();
                sender.sendMessage(Component.text("AdminActionLogger 설정이 리로드되었습니다.").color(NamedTextColor.GREEN));
                break;
                
            case "status":
                sendStatus(sender);
                break;
                
            case "debug":
                boolean debugState = !plugin.getConfig().getBoolean("debug-mode", false);
                plugin.getConfig().set("debug-mode", debugState);
                plugin.saveConfig();
                
                if (debugState) {
                    sender.sendMessage(Component.text("디버그 모드가 활성화되었습니다. 모든 이벤트 세부 정보가 콘솔에 기록됩니다.").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("디버그 모드가 비활성화되었습니다.").color(NamedTextColor.GREEN));
                }
                break;
                
            case "logs":
                if (args.length > 1 && args[1].equalsIgnoreCase("view")) {
                    sendLogSummary(sender);
                    return true;
                }
                
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    clearLogs(sender);
                    return true;
                }
                
                sender.sendMessage(
                    Component.text("/clog logs view").color(NamedTextColor.YELLOW)
                        .append(Component.text(" - 최근 로그 확인").color(NamedTextColor.WHITE))
                );
                sender.sendMessage(
                    Component.text("/clog logs clear").color(NamedTextColor.YELLOW)
                        .append(Component.text(" - 로그 파일 초기화 (주의: 복구 불가)").color(NamedTextColor.WHITE))
                );
                break;
                
            case "monitor":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("사용법: /clog monitor <add|remove> <플레이어이름>").color(NamedTextColor.RED));
                    return true;
                }
                
                String subCommand = args[1].toLowerCase();
                String playerName = args[2];
                
                if (subCommand.equals("add")) {
                    List<String> monitored = plugin.getConfig().getStringList("monitored-players");
                    if (!monitored.contains(playerName)) {
                        monitored.add(playerName);
                        plugin.getConfig().set("monitored-players", monitored);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text(playerName + "님을 모니터링 대상에 추가했습니다.").color(NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text(playerName + "님은 이미 모니터링 대상입니다.").color(NamedTextColor.YELLOW));
                    }
                } else if (subCommand.equals("remove")) {
                    List<String> monitored = plugin.getConfig().getStringList("monitored-players");
                    if (monitored.remove(playerName)) {
                        plugin.getConfig().set("monitored-players", monitored);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text(playerName + "님을 모니터링 대상에서 제외했습니다.").color(NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text(playerName + "님은 모니터링 대상이 아닙니다.").color(NamedTextColor.YELLOW));
                    }
                } else {
                    sender.sendMessage(Component.text("사용법: /clog monitor <add|remove> <플레이어이름>").color(NamedTextColor.RED));
                }
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== AdminActionLogger 명령어 =====").color(NamedTextColor.GREEN));
        sender.sendMessage(
            Component.text("/clog toggle").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 로깅 기능 활성화/비활성화").color(NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("/clog reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 설정 리로드").color(NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("/clog status").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 현재 상태 확인").color(NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("/clog debug").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 디버그 모드 활성화/비활성화").color(NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("/clog logs view").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 최근 로그 확인").color(NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("/clog monitor <add|remove> <플레이어>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 모니터링 대상 추가/제거").color(NamedTextColor.WHITE))
        );
    }
    
    private void sendStatus(CommandSender sender) {
        // 기본 상태 정보
        Component statusComponent = Component.text("로깅 상태: ").color(NamedTextColor.GREEN);
        if (plugin.isLoggingEnabled()) {
            statusComponent = statusComponent.append(Component.text("활성화").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        } else {
            statusComponent = statusComponent.append(Component.text("비활성화").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        }
        sender.sendMessage(statusComponent);
        
        // 디버그 모드 상태
        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        Component debugComponent = Component.text("디버그 모드: ").color(NamedTextColor.GREEN);
        if (debugMode) {
            debugComponent = debugComponent.append(Component.text("활성화").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        } else {
            debugComponent = debugComponent.append(Component.text("비활성화").color(NamedTextColor.GRAY));
        }
        sender.sendMessage(debugComponent);
        
        // 모니터링 대상 플레이어
        List<String> monitoredPlayers = plugin.getConfig().getStringList("monitored-players");
        if (monitoredPlayers.isEmpty()) {
            sender.sendMessage(Component.text("모니터링 대상: ").color(NamedTextColor.GREEN)
                .append(Component.text("모든 OP 및 gamemode 권한 보유자").color(NamedTextColor.GRAY)));
        } else {
            sender.sendMessage(Component.text("모니터링 대상: ").color(NamedTextColor.GREEN)
                .append(Component.text(String.join(", ", monitoredPlayers)).color(NamedTextColor.WHITE)));
        }
        
        // 활성화된 기능
        List<Component> enabledFeatures = new ArrayList<>();
        if (plugin.getConfig().getBoolean("log-gamemode-changes", true)) {
            enabledFeatures.add(Component.text("게임모드 변경").color(NamedTextColor.WHITE));
        }
        if (plugin.getConfig().getBoolean("log-item-transfers", true)) {
            enabledFeatures.add(Component.text("아이템 이동").color(NamedTextColor.WHITE));
        }
        if (plugin.getConfig().getBoolean("log-sensitive-commands", true)) {
            enabledFeatures.add(Component.text("민감한 명령어").color(NamedTextColor.WHITE));
        }
        if (plugin.getConfig().getBoolean("log-join-quit", true)) {
            enabledFeatures.add(Component.text("접속/퇴장").color(NamedTextColor.WHITE));
        }
        
        Component featuresComponent = Component.text("활성화된 기능: ").color(NamedTextColor.GREEN);
        for (int i = 0; i < enabledFeatures.size(); i++) {
            featuresComponent = featuresComponent.append(enabledFeatures.get(i));
            if (i < enabledFeatures.size() - 1) {
                featuresComponent = featuresComponent.append(Component.text(", ").color(NamedTextColor.GRAY));
            }
        }
        sender.sendMessage(featuresComponent);
        
        // 로그 파일 정보
        if (plugin.getConfig().getBoolean("log-to-file", true)) {
            File logFile = new File(plugin.getDataFolder(), 
                plugin.getConfig().getString("log-file.filename", "admin-actions.log"));
            
            String fileSize = "0KB";
            if (logFile.exists()) {
                fileSize = String.format("%.2f KB", logFile.length() / 1024.0);
            }
            
            sender.sendMessage(Component.text("로그 파일: ").color(NamedTextColor.GREEN)
                .append(Component.text(logFile.getName() + " (" + fileSize + ")").color(NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("로그 파일: ").color(NamedTextColor.GREEN)
                .append(Component.text("비활성화").color(NamedTextColor.GRAY)));
        }
    }
    
    private void sendLogSummary(CommandSender sender) {
        File logFile = new File(plugin.getDataFolder(), 
            plugin.getConfig().getString("log-file.filename", "admin-actions.log"));
        
        if (!logFile.exists() || !logFile.canRead()) {
            sender.sendMessage(Component.text("로그 파일을 읽을 수 없습니다.").color(NamedTextColor.RED));
            return;
        }
        
        sender.sendMessage(Component.text("===== 최근 로그 요약 =====").color(NamedTextColor.GREEN));
        
        // 이 부분은 실제로 로그 파일에서 최근 로그 일부를 읽어 표시하는 코드를 구현해야 합니다.
        // 여기서는 간단히 예시 메시지만 표시합니다.
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        sender.sendMessage(Component.text("오늘 로그 파일: " + currentDate).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("전체 로그는 서버 콘솔이나 로그 파일에서 확인하세요.").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("로그 파일 위치: plugins/AdminActionLogger/admin-actions.log").color(NamedTextColor.GRAY));
    }
    
    private void clearLogs(CommandSender sender) {
        File logFile = new File(plugin.getDataFolder(), 
            plugin.getConfig().getString("log-file.filename", "admin-actions.log"));
        
        if (!logFile.exists()) {
            sender.sendMessage(Component.text("로그 파일이 존재하지 않습니다.").color(NamedTextColor.YELLOW));
            return;
        }
        
        if (logFile.delete()) {
            sender.sendMessage(Component.text("로그 파일이 삭제되었습니다.").color(NamedTextColor.GREEN));
            try {
                logFile.createNewFile();
                sender.sendMessage(Component.text("새 로그 파일이 생성되었습니다.").color(NamedTextColor.GREEN));
            } catch (Exception e) {
                sender.sendMessage(Component.text("새 로그 파일 생성 중 오류가 발생했습니다: " + e.getMessage()).color(NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("로그 파일을 삭제할 수 없습니다.").color(NamedTextColor.RED));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("creativeitemlogger.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList(
                "toggle", "reload", "status", "debug", "logs", "monitor"
            ));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("logs")) {
                List<String> subCommands = Arrays.asList("view", "clear");
                return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
            
            if (args[0].equalsIgnoreCase("monitor")) {
                List<String> subCommands = Arrays.asList("add", "remove");
                return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("monitor")) {
            if (args[1].equalsIgnoreCase("add")) {
                // 모니터링 대상이 아닌 온라인 플레이어 목록 반환
                List<String> monitoredPlayers = plugin.getConfig().getStringList("monitored-players");
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !monitoredPlayers.contains(name))
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
            } else if (args[1].equalsIgnoreCase("remove")) {
                // 모니터링 중인 플레이어 목록 반환
                List<String> monitoredPlayers = plugin.getConfig().getStringList("monitored-players");
                return monitoredPlayers.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
            }
        }
        
        return new ArrayList<>();
    }
}