package hexlet.code.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public final class Url extends Model {
    @Id
    private long id;
    private String name;
    @WhenCreated
    private String createdAt;

    public Url(String urlName) {
        this.name = urlName;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
