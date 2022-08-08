package io.opsit.explang;
import java.util.Random;

public class TestBean {
  {
    String names[] = {"Eric","Donald","Richard","Guy","Ada"};
    String surnames[] = {"Stallman","Knuth","MacCarthy","Raymond","Steel","Lovelace"};
    Random r = new Random();
    name = names[r.nextInt(names.length)];
    surName = surnames[r.nextInt(surnames.length)];
    age = r.nextInt(120);
    accepted = r.nextBoolean();
    children = r.nextInt(10);
  }
    
  private String name;
  private String surName;
  private Integer age;
  private int children;
  private Boolean accepted;

  public String getName() {
    return name;
  }

  public String getSurName() {
    return surName;
  }

    
  public Integer getAge() {
    return age;
  }

  public int  getChildren() {
    return children;
  }

    
  public Boolean isAccepted() {
    return accepted;
  }

  public String toString() {
    return "<TestBean>(name: " + getName() +
      " surName: " + getSurName() +
      " age: "+ getAge() +
      " accepted: " + isAccepted() +")";
  }
}
