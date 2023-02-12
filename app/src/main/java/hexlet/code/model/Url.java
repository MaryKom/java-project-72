package hexlet.code.model;

import io.ebean.Model;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Url extends Model {
    @Id
    private long id;
    private String name;
    private String createdAt;
}
