package com.nutriconsultas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.nutriconsultas.controller.WebController;

@SpringBootTest
public class SmokeTest {
  @Autowired
  WebController controller;

  @Test
  public void contextLoads() throws Exception {
    assertThat(controller).isNotNull();
  }
}
