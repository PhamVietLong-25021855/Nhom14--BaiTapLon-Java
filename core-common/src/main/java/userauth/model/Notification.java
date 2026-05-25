package userauth.model;

public class Notification extends Entity {
    private int user_id;
    private String title;
    private String content;
    private long created_at;

    public Notification(int id, int user_id, String title, String content, long created_at) {
        super(id);
        this.user_id = user_id;
        this.title = title;
        this.content = content;
        this.created_at = created_at;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }
}
