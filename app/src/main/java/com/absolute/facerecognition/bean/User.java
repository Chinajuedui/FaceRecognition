package com.absolute.facerecognition.bean;

public class User {

    private String face_token;
    private location loc;
    private user_list user_list;

    public String getFace_token() {
        return face_token;
    }

    public void setFace_token(String face_token) {
        this.face_token = face_token;
    }

    public location getLocation() {
        return loc;
    }

    public void setLocation(location loc) {
        this.loc = loc;
    }

    public user_list getUser_list() {
        return user_list;
    }

    public void setUser_list(user_list user_list) {
        this.user_list = user_list;
    }

    class location {
        double left;
        double top;
        double width;
        double height;
        double rotation;
    }

    public class user_list {
        String group_id;
        String user_id;
        String user_info;
        String score;

        public String getUser_id() {
            return user_id;
        }

        public void setUser_id(String user_id) {
            this.user_id = user_id;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "face_token='" + face_token + '\'' +
                ", location=" + loc +
                ", user_list=" + user_list +
                '}';
    }
}
