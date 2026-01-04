package jomeerkatz.pm.patient_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name can't exceed 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email should not be blank")
    private String email;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "date of birth is required")
    private String dateOfBirth;

    @NotBlank(message = "registered date is required")
    private String registeredDate;
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }
}
