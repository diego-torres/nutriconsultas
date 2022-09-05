package com.nutriconsultas.dataTables.paging;

public class ComparatorKey {
  String name;
  Direction dir;

  public ComparatorKey(String name, Direction dir) {
    this.name = name;
    this.dir = dir;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Direction getDir() {
    return dir;
  }

  public void setDir(Direction dir) {
    this.dir = dir;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dir == null) ? 0 : dir.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ComparatorKey other = (ComparatorKey) obj;
    if (dir != other.dir)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  
}
