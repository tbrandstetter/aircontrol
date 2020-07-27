package at.co.brandstetter.aircontrolservice.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="register")
public class RegisterEntity implements Serializable {

    @Id
    @Column(name = "register")
    private int register;
    @Column(name = "value")
    private String value;
    @Column(name = "description")
    private String description;
    @Column(name = "min")
    private int min;
    @Column(name = "max")
    private int max;
    @Column(name = "divisor")
    private int divisor;
    @Column(name = "lastupdate")
    private LocalDateTime lastupdate;
}
