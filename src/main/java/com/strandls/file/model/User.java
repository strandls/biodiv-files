/**
 * 
 */
package com.strandls.file.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

/**
 * @author Abhishek Rudra
 *
 */
@ApiModel
@Entity
@Table(name = "suser")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "password" })
public class User implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6815897629561065464L;

	private Long id;
	private Long version;
	private Boolean accountExpired;
	private Boolean accountLocked;
	private Boolean passwordExpired;
	private Long languageId;
	private Boolean enabled;
	private String userName;
	private String aboutMe;
	private String email;
	private Boolean hideEmial;
	private String name;
	private String profilePic;
	private String icon;
	private String password;
	private String sexType;
	private Date dateCreated;
	private Double latitude;
	private Double longitude;
	private String mobileNumber;
	private String occupation;
	private String institution;
	private String location;
	private Boolean sendNotification;
	private Boolean emailValidation;
	private Boolean mobileValidation;
	private Date lastLoginDate;
	private Set<Role> roles;
	private Float timezone;
	private Boolean identificationMail;
	private Boolean sendDigest;
	private Boolean sendPushNotification;
	private Set<FirebaseTokens> tokens;

	public User() {
	}

	public User(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Column(name = "language_id")
	public Long getLanguageId() {
		return languageId;
	}

	public void setLanguageId(Long languageId) {
		this.languageId = languageId;
	}

	@Column(name = "password_expired")
	public Boolean getPasswordExpired() {
		return passwordExpired;
	}

	public void setPasswordExpired(Boolean passwordExpired) {
		this.passwordExpired = passwordExpired;
	}

	@Column(name = "password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column(name = "account_expired")
	public Boolean getAccountExpired() {
		return accountExpired;
	}

	public void setAccountExpired(Boolean accountExpired) {
		this.accountExpired = accountExpired;
	}

	@Column(name = "account_locked")
	public Boolean getAccountLocked() {
		return accountLocked;
	}

	public void setAccountLocked(Boolean accountLocked) {
		this.accountLocked = accountLocked;
	}

	@Column(name = "enabled")
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Column(name = "last_login_date")
	public Date getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Date lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	@Column(name = "username")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column(name = "about_me")
	public String getAboutMe() {
		return aboutMe;
	}

	public void setAboutMe(String aboutMe) {
		this.aboutMe = aboutMe;
	}

	@Column(name = "email")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column(name = "hide_email_id")
	public Boolean getHideEmial() {
		return hideEmial;
	}

	public void setHideEmial(Boolean hideEmial) {
		this.hideEmial = hideEmial;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "profile_pic")
	public String getProfilePic() {
		return profilePic;
	}

	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}

	@Column(name = "icon")
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "suser_role", joinColumns = { @JoinColumn(name = "s_user_id") }, inverseJoinColumns = {
			@JoinColumn(name = "role_id") })
	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	@Column(name = "mobile_number")
	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	@Column(name = "sex_type")
	public String getSexType() {
		return sexType;
	}

	public void setSexType(String sexType) {
		this.sexType = sexType;
	}

	@Column(name = "latitude")
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	@Column(name = "longitude")
	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	@Column(name = "occupation_type")
	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	@Column(name = "institution_type")
	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	@Column(name = "location")
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Column(name = "date_created")
	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Column(name = "send_notification")
	public Boolean getSendNotification() {
		return sendNotification;
	}

	public void setSendNotification(Boolean sendNotification) {
		this.sendNotification = sendNotification;
	}

	@Column(name = "email_validation")
	public Boolean getEmailValidation() {
		return emailValidation;
	}

	public void setEmailValidation(Boolean emailValidation) {
		this.emailValidation = emailValidation;
	}

	@Column(name = "mobile_validation")
	public Boolean getMobileValidation() {
		return mobileValidation;
	}

	public void setMobileValidation(Boolean mobileValidation) {
		this.mobileValidation = mobileValidation;
	}

	@Column(name = "timezone")
	public Float getTimezone() {
		return timezone;
	}

	public void setTimezone(Float timezone) {
		this.timezone = timezone;
	}

	@Column(name = "allow_identifaction_mail")
	public Boolean getIdentificationMail() {
		return identificationMail;
	}

	public void setIdentificationMail(Boolean identificationMail) {
		this.identificationMail = identificationMail;
	}

	@Column(name = "send_digest")
	public Boolean getSendDigest() {
		return sendDigest;
	}

	public void setSendDigest(Boolean sendDigest) {
		this.sendDigest = sendDigest;
	}
	
	@Column(name = "send_push_notification", columnDefinition = "boolean default false")
	public Boolean getSendPushNotification() {
		return sendPushNotification;
	}
	
	public void setSendPushNotification(Boolean sendPushNotification) {
		this.sendPushNotification = sendPushNotification;
	}
	
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
	public Set<FirebaseTokens> getTokens() {
		return tokens;
	}
	
	public void setTokens(Set<FirebaseTokens> tokens) {
		this.tokens = tokens;
	}

}
