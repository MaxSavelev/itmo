package client.gui;

import data.Vehicle;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Панель визуализации объектов Vehicle.
 */
public final class VehicleCanvasPanel extends JPanel {
    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 1000;
    private static final int MARGIN = 36;
    private static final int GRID_STEP = 50;
    private static final int LABEL_GAP = 16;
    private static final int LABEL_LINE_HEIGHT = 14;
    private static final int ANIMATION_DELAY_MS = 30;
    private static final double ANIMATION_STEP = 0.08d;
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color GRID_COLOR = new Color(0xE5E7EB);
    private static final Color AXIS_COLOR = new Color(0xCBD5E1);
    private static final List<Color> OWNER_PALETTE = List.of(
            new Color(0x2F6BFF),
            new Color(0x22A06B),
            new Color(0xF97316),
            new Color(0x8B5CF6),
            new Color(0x0EA5E9),
            new Color(0xDB2777),
            new Color(0x16A34A),
            new Color(0xD97706)
    );

    private final Map<String, Color> ownerColors = new HashMap<>();
    private final Map<Vehicle, Ellipse2D> vehicleShapes = new LinkedHashMap<>();
    private final Set<Integer> animatedVehicleIds = new HashSet<>();
    private final Map<Integer, Double> animationProgress = new HashMap<>();
    private final Timer animationTimer;
    private List<Vehicle> vehicles = List.of();
    private Vehicle selectedVehicle;
    private Consumer<Vehicle> vehicleSelectionListener = vehicle -> {
    };
    private double coordinateOffsetX;
    private double coordinateOffsetY;

    public VehicleCanvasPanel() {
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        animationTimer = new Timer(ANIMATION_DELAY_MS, event -> advanceAnimations());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                handleMouseClick(event.getPoint());
            }
        });
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = List.copyOf(Objects.requireNonNullElse(vehicles, List.of()));
        updateCoordinateOffsets();
        updatePreferredSize();
        rebuildVehicleShapes();
        repaint();
    }

    public void setSelectedVehicle(Vehicle selectedVehicle) {
        this.selectedVehicle = selectedVehicle;
        repaint();
    }

    public void startAppearanceAnimation(Collection<Integer> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return;
        }
        for (Integer vehicleId : vehicleIds) {
            if (vehicleId != null) {
                animatedVehicleIds.add(vehicleId);
                animationProgress.put(vehicleId, 0.0d);
            }
        }
        if (!animatedVehicleIds.isEmpty() && !animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    public void setVehicleSelectionListener(Consumer<Vehicle> vehicleSelectionListener) {
        this.vehicleSelectionListener = Objects.requireNonNullElse(vehicleSelectionListener, vehicle -> {
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BACKGROUND);
            g2.fillRect(0, 0, getWidth(), getHeight());
            drawGrid(g2);

            for (Vehicle vehicle : vehicles) {
                drawVehicle(g2, vehicle);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(GRID_COLOR);
        for (int x = MARGIN; x < getWidth(); x += GRID_STEP) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = MARGIN; y < getHeight(); y += GRID_STEP) {
            g2.drawLine(0, y, getWidth(), y);
        }

        g2.setColor(AXIS_COLOR);
        g2.drawLine(MARGIN, 0, MARGIN, getHeight());
        g2.drawLine(0, MARGIN, getWidth(), MARGIN);
    }

    private void drawVehicle(Graphics2D g2, Vehicle vehicle) {
        int size = calculateSize(vehicle);
        int x = calculateScreenX(vehicle);
        int y = calculateScreenY(vehicle);
        boolean selected = selectedVehicle != null && selectedVehicle.getId().equals(vehicle.getId());
        Color ownerColor = getOwnerColor(vehicle.getOwnerLogin());

        Ellipse2D shape = createAnimatedShape(vehicle);
        g2.setColor(ownerColor);
        g2.fill(shape);

        g2.setColor(selected ? Color.BLACK : ownerColor.darker());
        g2.setStroke(new BasicStroke(selected ? 4f : 2f));
        g2.draw(shape);

        g2.setColor(Color.DARK_GRAY);
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + Math.max(0, (size - metrics.stringWidth(vehicle.getName())) / 2);
        int firstLineY = y + size + LABEL_GAP;
        g2.drawString(vehicle.getName(), textX, firstLineY);

        String ownerLogin = vehicle.getOwnerLogin() == null ? "-" : vehicle.getOwnerLogin();
        int ownerTextX = x + Math.max(0, (size - metrics.stringWidth(ownerLogin)) / 2);
        g2.drawString(ownerLogin, ownerTextX, firstLineY + LABEL_LINE_HEIGHT);
    }

    private void advanceAnimations() {
        Iterator<Integer> iterator = animatedVehicleIds.iterator();
        while (iterator.hasNext()) {
            Integer vehicleId = iterator.next();
            double nextProgress = animationProgress.getOrDefault(vehicleId, 1.0d) + ANIMATION_STEP;
            if (nextProgress >= 1.0d) {
                iterator.remove();
                animationProgress.remove(vehicleId);
            } else {
                animationProgress.put(vehicleId, nextProgress);
            }
        }
        if (animatedVehicleIds.isEmpty()) {
            animationTimer.stop();
        }
        repaint();
    }

    private void handleMouseClick(Point point) {
        Vehicle clickedVehicle = findVehicleAt(point);
        if (clickedVehicle != null) {
            setSelectedVehicle(clickedVehicle);
            vehicleSelectionListener.accept(clickedVehicle);
        }
    }

    private Vehicle findVehicleAt(Point point) {
        for (int i = vehicles.size() - 1; i >= 0; i--) {
            Vehicle vehicle = vehicles.get(i);
            Ellipse2D shape = vehicleShapes.get(vehicle);
            if (shape != null && shape.contains(point)) {
                return vehicle;
            }
        }
        return null;
    }

    private void rebuildVehicleShapes() {
        vehicleShapes.clear();
        for (Vehicle vehicle : vehicles) {
            vehicleShapes.put(vehicle, createShape(vehicle));
        }
    }

    private Ellipse2D createShape(Vehicle vehicle) {
        int size = calculateSize(vehicle);
        int x = calculateScreenX(vehicle);
        int y = calculateScreenY(vehicle);
        return new Ellipse2D.Double(x, y, size, size);
    }

    private Ellipse2D createAnimatedShape(Vehicle vehicle) {
        double progress = getAnimationProgress(vehicle);
        if (progress >= 1.0d) {
            return vehicleShapes.getOrDefault(vehicle, createShape(vehicle));
        }

        int normalSize = calculateSize(vehicle);
        double animatedSize = Math.max(1.0d, normalSize * progress);
        double normalX = calculateScreenX(vehicle);
        double normalY = calculateScreenY(vehicle);
        double offset = (normalSize - animatedSize) / 2.0d;
        return new Ellipse2D.Double(normalX + offset, normalY + offset, animatedSize, animatedSize);
    }

    private double getAnimationProgress(Vehicle vehicle) {
        Integer vehicleId = vehicle.getId();
        if (vehicleId == null) {
            return 1.0d;
        }
        return animationProgress.getOrDefault(vehicleId, 1.0d);
    }

    private int calculateSize(Vehicle vehicle) {
        double capacity = Math.max(0.0d, vehicle.getCapacity());
        int size = (int) Math.round(24.0d + Math.sqrt(capacity) * 2.5d);
        return Math.max(24, Math.min(96, size));
    }

    private Color getOwnerColor(String ownerLogin) {
        String key = ownerLogin == null ? "<unknown>" : ownerLogin;
        return ownerColors.computeIfAbsent(key, ignored -> OWNER_PALETTE.get(ownerColors.size() % OWNER_PALETTE.size()));
    }

    private void updateCoordinateOffsets() {
        double minX = 0.0d;
        double minY = 0.0d;
        for (Vehicle vehicle : vehicles) {
            minX = Math.min(minX, vehicle.getCoordinates().getX());
            minY = Math.min(minY, vehicle.getCoordinates().getY());
        }
        coordinateOffsetX = minX < 0.0d ? -minX : 0.0d;
        coordinateOffsetY = minY < 0.0d ? -minY : 0.0d;
    }

    private int calculateScreenX(Vehicle vehicle) {
        return MARGIN + clampToCanvasCoordinate(vehicle.getCoordinates().getX() + coordinateOffsetX);
    }

    private int calculateScreenY(Vehicle vehicle) {
        return MARGIN + clampToCanvasCoordinate(vehicle.getCoordinates().getY() + coordinateOffsetY);
    }

    private int clampToCanvasCoordinate(double value) {
        if (value > Integer.MAX_VALUE - MARGIN) {
            return Integer.MAX_VALUE - MARGIN;
        }
        return Math.max(0, (int) Math.round(value));
    }

    private void updatePreferredSize() {
        int maxRight = DEFAULT_WIDTH;
        int maxBottom = DEFAULT_HEIGHT;
        for (Vehicle vehicle : vehicles) {
            int size = calculateSize(vehicle);
            int x = calculateScreenX(vehicle);
            int y = calculateScreenY(vehicle);
            maxRight = Math.max(maxRight, x + size + MARGIN);
            maxBottom = Math.max(maxBottom, y + size + MARGIN + LABEL_GAP + LABEL_LINE_HEIGHT * 2);
        }
        setPreferredSize(new Dimension(maxRight, maxBottom));
        revalidate();
    }
}
