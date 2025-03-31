import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.stage.Stage;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;

public class JavaFX3DScene extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Создаем корневой узел сцены
        Group root = new Group();

        // Создаем 3D сцену
        Scene scene = new Scene(root, 800, 600, true);
        scene.setFill(Color.LIGHTGRAY);

        // Создаем камеру
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1000);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(30);
        scene.setCamera(camera);

        // Создаем главный объект - сферу (можете заменить на другую фигуру)
        Sphere sphere = new Sphere(150);
        sphere.setTranslateX(0);
        sphere.setTranslateY(0);
        sphere.setTranslateZ(0);

        // Настраиваем материал сферы
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.BLUE);
        material.setSpecularColor(Color.LIGHTBLUE);
        material.setSpecularPower(32);
        sphere.setMaterial(material);

        // Добавляем вращение сфере для лучшего 3D эффекта
        Rotate rotateX = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
        sphere.getTransforms().addAll(rotateX, rotateY);

        // Анимация вращения
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(rotateX.angleProperty(), 0),
                        new KeyValue(rotateY.angleProperty(), 0)),
                new KeyFrame(Duration.seconds(10),
                        new KeyValue(rotateX.angleProperty(), 360),
                        new KeyValue(rotateY.angleProperty(), 360))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Добавляем освещение
        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(-300);
        light.setTranslateY(-200);
        light.setTranslateZ(-500);

        // Добавляем все объекты в сцену
        root.getChildren().addAll(sphere, light);

        // Настройка и отображение окна
        primaryStage.setTitle("JavaFX 3D Scene");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}