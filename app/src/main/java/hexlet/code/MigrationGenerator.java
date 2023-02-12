package hexlet.code;

import java.io.IOException;
import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
public class MigrationGenerator {
    public static void main(String[] args) throws IOException {
        // Создаём миграцию
        DbMigration dbMigration = DbMigration.create();
        // Указываем платформу, в нашем случае H2
        dbMigration.addPlatform(Platform.H2, "h2");
        dbMigration.addPlatform(Platform.POSTGRES, "postgres");
        // Генерируем миграцию
        dbMigration.generateMigration();
    }
}
