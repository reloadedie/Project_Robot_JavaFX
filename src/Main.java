import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.image.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import java.util.*;
import javafx.geometry.Point3D;
import java.io.*;

public class Main extends Application {

    // Параметры вращения
    private final Rotate rotateX = new Rotate(0, new Point3D(1, 0, 0));
    private final Rotate rotateY = new Rotate(0, new Point3D(0, 1, 0));
    private final Rotate rotateZ = new Rotate(0, new Point3D(0, 0, 1));

    // Настройки управления
    private double currentAngleX = 0, currentAngleY = 0, currentAngleZ = 0;
    private final double smoothingFactor = 0.15;
    private final double sensitivityThreshold = 0.2;
    private final double[] sensitivityXYZ = {10.0, 10.0, 5.0};

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Инициализация сцены
        Group root = new Group();
        Scene scene = new Scene(root, 1000, 800, true);
        scene.setFill(Color.LIGHTGRAY);

        // 2. Настройка камеры (важные параметры)
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(55);
        scene.setCamera(camera);

        // 3. Загрузка модели с расширенной диагностикой
        Group model = load3DModel();
        if (model == null) {
            System.err.println("Не удалось загрузить основную модель, используется резервная");
            model = createDetailedFallbackModel();
        }

        // 4. Настройка трансформаций
        Group rotationGroup = new Group(model);
        rotationGroup.getTransforms().addAll(rotateZ, rotateY, rotateX);
        root.getChildren().addAll(createCoordinateSystem(), rotationGroup);

        // 5. Настройка освещения (3 источника)
        setupAdvancedLighting(root);

        // 6. Настройка окна
        primaryStage.setTitle("3D Viewer with Finch Control");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 7. Инициализация управления (если Finch подключен)
        setupControlSystem(primaryStage);
    }

    private Group load3DModel() {
        try {
            // 1. Проверка существования файла
            InputStream modelStream = getClass().getResourceAsStream("/girl/girl.obj");
            if (modelStream == null) {
                System.err.println("Файл модели не найден в /girl/girl.obj");
                return null;
            }
            modelStream.close();

            // 2. Загрузка модели
            ObjModelImporter importer = new ObjModelImporter();
            importer.read(getClass().getResource("/girl/girl.obj").toExternalForm());

            // 3. Проверка загруженных данных
            Node[] meshViews = importer.getImport();
            if (meshViews == null || meshViews.length == 0) {
                System.err.println("Модель не содержит мешей");
                return null;
            }

            // 4. Создание группы и настройка
            Group modelGroup = new Group(meshViews);
            modelGroup.setScaleX(2.0);
            modelGroup.setScaleY(2.0);
            modelGroup.setScaleZ(2.0);
            modelGroup.setTranslateY(100);

            // 5. Загрузка и назначение текстур
            applyTextures(modelGroup);

            return modelGroup;

        } catch (Exception e) {
            System.err.println("Ошибка загрузки модели: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void applyTextures(Group modelGroup) {
        // 1. Список возможных текстур
        String[] textureNames = {"body", "face", "hair", "skin", "clothes"};

        // 2. Загрузка доступных текстур
        Map<String, Image> textures = new HashMap<>();
        for (String name : textureNames) {
            try {
                String path = "/girl/Textures/" + name + ".png";
                InputStream texStream = getClass().getResourceAsStream(path);
                if (texStream != null) {
                    Image img = new Image(texStream);
                    textures.put(name, img);
                    System.out.println("Загружена текстура: " + name);
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки текстуры " + name + ": " + e.getMessage());
            }
        }

        // 3. Применение материалов
        for (Node node : modelGroup.getChildren()) {
            if (node instanceof MeshView) {
                MeshView mesh = (MeshView) node;
                PhongMaterial material = new PhongMaterial();
                material.setDiffuseColor(Color.LIGHTGRAY);
                material.setSpecularColor(Color.WHITE);
                material.setSpecularPower(20);

                // Поиск подходящей текстуры по имени меша
                if (mesh.getId() != null) {
                    String meshId = mesh.getId().toLowerCase();
                    for (String texName : textures.keySet()) {
                        if (meshId.contains(texName)) {
                            material.setDiffuseMap(textures.get(texName));
                            break;
                        }
                    }
                }

                mesh.setMaterial(material);
            }
        }
    }

    private Group createDetailedFallbackModel() {
        Group fallback = new Group();

        // Тело
        Cylinder body = new Cylinder(40, 120);
        body.setMaterial(new PhongMaterial(Color.PINK));
        body.setTranslateY(-60);
        body.setRotationAxis(Rotate.X_AXIS);
        body.setRotate(90);

        // Голова
        Sphere head = new Sphere(35);
        head.setMaterial(new PhongMaterial(Color.PEACHPUFF));
        head.setTranslateY(-120);

        // Глаза
        Sphere leftEye = new Sphere(5);
        leftEye.setMaterial(new PhongMaterial(Color.WHITE));
        leftEye.setTranslateX(-15);
        leftEye.setTranslateY(-130);
        leftEye.setTranslateZ(-25);

        Sphere rightEye = new Sphere(5);
        rightEye.setMaterial(new PhongMaterial(Color.WHITE));
        rightEye.setTranslateX(15);
        rightEye.setTranslateY(-130);
        rightEye.setTranslateZ(-25);

        fallback.getChildren().addAll(body, head, leftEye, rightEye);
        return fallback;
    }

    private void setupAdvancedLighting(Group root) {
        // Основной свет
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-400);
        keyLight.setTranslateY(-300);
        keyLight.setTranslateZ(-500);

        // Заполняющий свет
        PointLight fillLight = new PointLight(Color.WHITESMOKE);
        fillLight.setTranslateX(400);
        fillLight.setTranslateY(200);
        fillLight.setTranslateZ(-400);

        // Фоновое освещение
        AmbientLight ambient = new AmbientLight(Color.rgb(100, 100, 100));

        root.getChildren().addAll(keyLight, fillLight, ambient);
    }

    private Group createCoordinateSystem() {
        Group axes = new Group();

        // Ось X (красная)
        Box xAxis = new Box(300, 2, 2);
        xAxis.setMaterial(new PhongMaterial(Color.RED));

        // Ось Y (зеленая)
        Box yAxis = new Box(2, 300, 2);
        yAxis.setMaterial(new PhongMaterial(Color.GREEN));

        // Ось Z (синяя)
        Box zAxis = new Box(2, 2, 300);
        zAxis.setMaterial(new PhongMaterial(Color.BLUE));

        axes.getChildren().addAll(xAxis, yAxis, zAxis);
        return axes;
    }

    private void setupControlSystem(Stage stage) {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Здесь можно добавить управление от Finch
                // Или автоматическое вращение для теста:
                rotateY.setAngle(rotateY.getAngle() + 0.5);
            }
        };
        timer.start();

        stage.setOnCloseRequest(e -> timer.stop());
    }
}