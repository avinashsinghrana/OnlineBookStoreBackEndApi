package com.bridgelabz.bookstore.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bridgelabz.bookstore.enums.RoleType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user")
public class UserModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private long userId;

	@NotNull
	private String fullName;

	@NotNull
	@Column(unique = true)
	private String emailId;

	@NotNull
	private String mobileNumber;

	@NotNull
	private String password;

	@Column(columnDefinition = "boolean default false")
	private boolean isVerified;

	private ZoneId zid = ZoneId.systemDefault();
	@CreationTimestamp
	public ZonedDateTime registrationDate = ZonedDateTime.now(zid);

	@UpdateTimestamp
	public ZonedDateTime updatedAt;

	@Column(columnDefinition = "boolean default false")
	public boolean userStatus;
	@Column
	private String profileUrl;

	@ManyToMany(cascade = CascadeType.ALL,fetch=FetchType.LAZY)
	@JoinTable(name = "userbooks", joinColumns = { @JoinColumn(name = "user_id") }, inverseJoinColumns ={@JoinColumn(name = "book_id") })
	private List<BookModel> book;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "book_Id")
	private List<BookModel> books;
	
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	private RoleType roleType;

	@OneToMany(mappedBy = "user")
	public List<UserDetails> userDetails;

	public List<UserDetails> getListOfUserDetails() {
		return userDetails;
	}

	public void addUserDetails(UserDetails userDetail) {
		this.userDetails.add(userDetail);
	}

	public void removeUserDetails(UserDetails userDetail) {
		this.userDetails.remove(userDetail);
	}

	public UserModel(String fullName, String emailId, String mobileNumber, String password) {
		super();
		this.fullName = fullName;
		this.password = password;
		this.mobileNumber = mobileNumber;
		this.emailId = emailId;
	}

}
