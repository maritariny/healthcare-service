import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;
import ru.netology.patient.service.medical.MedicalService;
import ru.netology.patient.service.medical.MedicalServiceImpl;


import java.math.BigDecimal;
import java.time.LocalDate;

public class testMedicalServiceImpl {

    @Test
    void test() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule(), new ParameterNamesModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        PatientInfo info = new PatientInfo("Иван", "Петров", LocalDate.of(1980, 11, 26),
                new HealthInfo(new BigDecimal("36.6"), new BloodPressure(120, 60)));

        PatientInfoRepository patientInfoRepository = Mockito.mock(PatientInfoRepository.class);
        String testId = "111";
        Mockito.when(patientInfoRepository.add(Mockito.any())).thenReturn(testId);
        Mockito.when(patientInfoRepository.getById(testId)).thenReturn(info);

        String id1 = patientInfoRepository.add(info);

        SendAlertService alertService = Mockito.mock(SendAlertService.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertService);

        // 1. Проверить вывод сообщения во время проверки давления checkBloodPressure
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        BloodPressure currentPressure = new BloodPressure(130, 60);
        medicalService.checkBloodPressure(id1, currentPressure);

        Mockito.verify(alertService, Mockito.times(1)).send(Mockito.any());

        Mockito.verify(alertService).send(argumentCaptor.capture());
        Assertions.assertTrue(argumentCaptor.getValue().contains("Warning, patient with id:"));

        // 2. Проверить вывод сообщения во время проверки температуры checkTemperature
        SendAlertService alertService3 = Mockito.mock(SendAlertService.class);
        BigDecimal currentTemperature = new BigDecimal("37.6");
        medicalService.checkTemperature(id1, currentTemperature);
        Mockito.verify(alertService3, Mockito.times(1)).send(Mockito.any());

        // 3. Проверить, что сообщения не выводятся, когда показатели в норме.
        SendAlertService alertService2 = Mockito.mock(SendAlertService.class);

        BloodPressure currentPressure2 = new BloodPressure(120, 60);
        medicalService.checkBloodPressure(id1, currentPressure2);
        Mockito.verify(alertService2, Mockito.times(0)).send(Mockito.any());

        BigDecimal currentTemperature2 = new BigDecimal("36.6");
        medicalService.checkTemperature(id1, currentTemperature2);
        Mockito.verify(alertService2, Mockito.times(0)).send(Mockito.any());

    }
}
