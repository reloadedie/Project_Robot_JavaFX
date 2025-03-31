import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class Finch2DInterface extends JPanel
{
    private Finch finch;
    private double angle = 0;

    public static void main(String[] args)
    {
        Finch finch = new Finch();
        new Finch2DInterface(finch);
    }

    public Finch2DInterface(Finch finch)
    {
        this.finch = finch;
        JFrame frame = new JFrame("Finch 2D Interface");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.add(this);
        frame.setVisible(true);

        // Запуск потока для обновления интерфейса
        new Thread(this::updateInterface).start();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Отрисовка фигуры (например, прямоугольника)
        int width = 100;
        int height = 50;
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;

        // Применение вращения
        AffineTransform old = g2d.getTransform();
        g2d.rotate(angle, x + width / 2.0, y + height / 2.0);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y, width, height);
        g2d.setTransform(old);
    }

    private void updateInterface()
    {
        while (true)
        {
            // Получение данных акселерометра
            double[] accelerations = finch.getAcceleration();
            double x = accelerations[0];
            double y = accelerations[1];

            // Вычисление угла вращения
            angle = Math.atan2(y, x);

            // Перерисовка интерфейса
            repaint();

            // Пауза для обновления интерфейса
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }


}