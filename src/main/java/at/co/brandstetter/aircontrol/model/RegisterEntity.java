package at.co.brandstetter.aircontrol.model;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="register")
public class RegisterEntity implements Serializable {

    @Id
    @Column(name = "register")
    private int register;
    @Column(name = "data")
    private String value;
    @Column(name = "description")
    private String description;
    @Column(name = "min")
    private int min;
    @Column(name = "max")
    private int max;
    @Column(name = "divisor")
    private int divisor;
    @Column(name = "updated")
    private LocalDateTime lastupdate;
}
