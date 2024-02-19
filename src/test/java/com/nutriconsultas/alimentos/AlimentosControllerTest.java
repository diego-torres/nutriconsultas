package com.nutriconsultas.alimentos;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
public class AlimentosControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlimentoService alimentoService;

    @BeforeEach
    public void setup() {
        // Read CSV file from classpath and convert to list of Alimento
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/alimentos.csv"))) {
            log.debug("reading alimentos from csv file");
            List<Alimento> alimentos = new CsvToBeanBuilder<Alimento>(reader)
                    .withType(Alimento.class).build().parse();
            log.debug("alimentos read from csv file: {}", alimentos.size());
            when(alimentoService.findById(784L)).thenReturn(alimentos.get(0));
            log.debug("alimentoService.findById(784L) returned: {}", alimentoService.findById(784L));
        } catch (IOException e) {
            log.error("error while reading alimentos from csv file", e);
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testListado() throws Exception {
        log.info("Starting testListado");
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/alimentos"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("sbadmin/alimentos/listado"))
                .andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "alimentos"));
        log.info("Finishing testListado");
    }

    @Test
    @SuppressWarnings({ "null" })
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testNuevoAlimento() throws Exception {
        log.info("Starting testNuevoAlimento");
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/alimentos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("sbadmin/alimentos/formulario"))
                .andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "alimentos"))
                .andExpect(MockMvcResultMatchers.model().attribute("alimento",
                        Matchers.<Alimento>hasProperty("clasificacion", Matchers.equalTo("ACEITES Y GRASAS"))));
        log.info("Finishing testNuevoAlimento");
    }

    @Test
    @SuppressWarnings({ "null" })
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testVerAlimento() throws Exception {
        log.info("Starting testVerAlimento");
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/alimentos/784"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("sbadmin/alimentos/formulario"))
                .andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "alimentos"))
                .andExpect(MockMvcResultMatchers.model().attribute("alimento",
                        Matchers.<Alimento>hasProperty("id", Matchers.equalTo(784L))))
                .andExpect(MockMvcResultMatchers.model().attribute("alimento",
                        Matchers.<Alimento>hasProperty("nombreAlimento", Matchers.equalTo("Naranja chica"))));
        log.info("Finishing testVerAlimento");
    }

    @Test
    @SuppressWarnings({ "null" })
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testAgregarNuevoAlimento() throws Exception {
        log.info("Starting testAgregarNuevoAlimento");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/alimentos")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("clasificacion","ACEITES Y GRASAS")
                .param("unidad","pieza")
                .param("nombreAlimento","Aceite de oliva")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/alimentos"));
        log.info("Finishing testAgregarNuevoAlimento");
    }
}
