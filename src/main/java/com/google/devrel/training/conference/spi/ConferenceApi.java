package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
//import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;

import java.util.ArrayList;
import java.util.Collection;
//import com.googlecode.objectify.annotation.Parent;
import java.util.List;

import javax.inject.Named;

import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = { Constants.WEB_CLIENT_ID,
		Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

	/*
	 * Get the display name from the user's email. For example, if the email is
	 * lemoncake@example.com, then the display name becomes "lemoncake."
	 */
	private static String extractDefaultDisplayNameFromEmail(String email) {
		return email == null ? null : email.substring(0, email.indexOf("@"));
	}

	/**
	 * Creates or updates a Profile object associated with the given user
	 * object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @param profileForm
	 *            A ProfileForm object sent from the client form.
	 * @return Profile object just created.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */

	// Declare this method as a method available externally through Endpoints
	@ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
	// The request that invokes this method should provide data that
	// conforms to the fields defined in ProfileForm

	// TODO 1 Pass the ProfileForm parameter
	// TODO 2 Pass the User parameter
	public Profile saveProfile(User u, ProfileForm form) throws UnauthorizedException {

		String userId = null;
		String mainEmail = null;
		String displayName = "Your Name Here";
		TeeShirtSize teeShirtSize = TeeShirtSize.NOT_SPECIFIED;

		// TODO 2
		// If the user is not logged in, throw an UnauthorizedException
		if (u == null)
			throw new UnauthorizedException("Please login.");

		// TODO 1
		// Set the teeShirtSize to the value sent by the ProfileForm, if sent
		// otherwise leave it as the default value

		if (form.getTeeShirtSize() != null)
			teeShirtSize = form.getTeeShirtSize();

		// TODO 1
		// Set the displayName to the value sent by the ProfileForm, if sent
		// otherwise set it to null

		if (form.getDisplayName() != null)
			displayName = form.getDisplayName();

		// TODO 2
		// Get the userId and mainEmail

		userId = u.getUserId();
		mainEmail = u.getEmail();

		// TODO 2
		// If the displayName is null, set it to default value based on the
		// user's email
		// by calling extractDefaultDisplayNameFromEmail(...)

		// Create a new Profile entity from the
		// userId, displayName, mainEmail and teeShirtSize
		Profile profile = getProfile(u);
		if (profile == null) {
			if (form.getDisplayName() == null)
				displayName = extractDefaultDisplayNameFromEmail(mainEmail);
			profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
		} else
			profile.update(displayName, teeShirtSize);

		// TODO 3 (In Lesson 3)
		// Save the Profile entity in the datastore
		ofy().save().entity(profile).now();

		// Return the profile
		return profile;
	}

	/**
	 * Returns a Profile object associated with the given user object. The cloud
	 * endpoints system automatically inject the User object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @return Profile object.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */
	@ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
	public Profile getProfile(final User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		// TODO
		// load the Profile Entity
		String userId = user.getUserId(); // TODO
		Key key = Key.create(Profile.class, userId); // TODO
		Profile profile = (Profile) ofy().load().key(key).now(); // TODO load
																	// the
																	// Profile
																	// entity
		return profile;
	}

	/**
	 * Gets the Profile entity for the current user or creates it if it doesn't
	 * exist
	 * 
	 * @param user
	 * @return user's Profile
	 */
	private static Profile getProfileFromUser(User user) {
		// First fetch the user's Profile from the datastore.
		Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
		if (profile == null) {
			// Create a new Profile if it doesn't exist.
			// Use default displayName and teeShirtSize
			String email = user.getEmail();
			profile = new Profile(user.getUserId(), extractDefaultDisplayNameFromEmail(email), email,
					TeeShirtSize.NOT_SPECIFIED);
		}
		return profile;
	}

	@ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
	public Conference createConference(final User user, final ConferenceForm conferenceForm)
			throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		// TODO (Lesson 4)
		// Get the userId of the logged in User
		String userId = user.getUserId();

		// TODO (Lesson 4)
		// Get the key for the User's Profile
		Key<Profile> profileKey = Key.create(Profile.class, userId);

		// TODO (Lesson 4)
		// Allocate a key for the conference -- let App Engine allocate the ID
		// Don't forget to include the parent Profile in the allocated ID
		final Key<Conference> conferenceKey = ofy().factory().allocateId(profileKey, Conference.class);

		// TODO (Lesson 4)
		// Get the Conference Id from the Key
		final long conferenceId = conferenceKey.getId();

		// TODO (Lesson 4)
		// Get the existing Profile entity for the current user if there is one
		// Otherwise create a new Profile entity with default values
		Profile profile = getProfileFromUser(user);

		// TODO (Lesson 4)
		// Create a new Conference Entity, specifying the user's Profile entity
		// as the parent of the conference
		Conference conference = new Conference(conferenceId, user.getUserId(), conferenceForm);

		// TODO (Lesson 4)
		// Save Conference and Profile Entities
		ofy().save().entities(profile, conference).now();

		return conference;
	}

	@ApiMethod(name = "queryConferences", path = "queryConferences", httpMethod = HttpMethod.POST)
	public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
		Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
		List<Conference> result = new ArrayList<>(0);
		List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
		for (Conference conference : conferenceIterable) {
			organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId()));
			result.add(conference);
		}
		// To avoid separate datastore gets for each Conference, pre-fetch the
		// Profiles.
		ofy().load().keys(organizersKeyList);
		return result;
	}

	@ApiMethod(name = "getConferencesCreated", path = "ConferencesCreated", httpMethod = HttpMethod.POST)
	public List<Conference> getConferencesCreated(User u) throws UnauthorizedException {
		if (u == null)
			throw new UnauthorizedException("You must login first.");
		String userId = u.getUserId();

		// TODO (Lesson 4)
		// Get the key for the User's Profile
		Key<Profile> profileKey = Key.create(Profile.class, userId);

		// TODO (Lesson 4)
		// Allocate a key for the conference -- let App Engine allocate the ID
		// Don't forget to include the parent Profile in the allocated ID
		final Key<Conference> conferenceKey = ofy().factory().allocateId(profileKey, Conference.class);

		Query<Conference> q = ofy().load().type(Conference.class).ancestor(conferenceKey);
		// List<Conference> con =
		// ofy().load().type(Conference.class).ancestor(conferenceKey);
		return q.list();
	}

	@ApiMethod(name = "getConferencesFiltered",
			path = "getConferencesFiltered",
			httpMethod = HttpMethod.POST)
	public List<Conference> getConferencesFiltered() {
		Query query = ofy().load().type(Conference.class);
		query = query.filter("maxAttendees >", 10);
		query = query.filter("city =", "London");
		query = query.filter("topics =", "Web Technologies");
		query = query.filter("month =", 1).order("maxAttendees").order("name");
		return query.list();
	}

	/**
	 * Returns a Conference object with the given conferenceId.
	 *
	 * @param websafeConferenceKey
	 *            The String representation of the Conference Key.
	 * @return a Conference object with the given conferenceId.
	 * @throws NotFoundException
	 *             when there is no Conference with the given conferenceId.
	 */
	@ApiMethod(name = "getConference", path = "conference/{websafeConferenceKey}", httpMethod = HttpMethod.GET)
	public Conference getConference(@Named("websafeConferenceKey") final String websafeConferenceKey)
			throws NotFoundException {
		Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
		Conference conference = ofy().load().key(conferenceKey).now();
		if (conference == null) {
			throw new NotFoundException("No Conference found with key: " + websafeConferenceKey);
		}
		return conference;
	}

	/**
	 * Just a wrapper for Boolean. We need this wrapped Boolean because
	 * endpoints functions must return an object instance, they can't return a
	 * Type class such as String or Integer or Boolean
	 */
	public static class WrappedBoolean {

		private final Boolean result;
		private final String reason;

		public WrappedBoolean(Boolean result) {
			this.result = result;
			this.reason = "";
		}

		public WrappedBoolean(Boolean result, String reason) {
			this.result = result;
			this.reason = reason;
		}

		public Boolean getResult() {
			return result;
		}

		public String getReason() {
			return reason;
		}
	}

	/**
	 * Register to attend the specified Conference.
	 *
	 * @param user
	 *            An user who invokes this method, null when the user is not
	 *            signed in.
	 * @param websafeConferenceKey
	 *            The String representation of the Conference Key.
	 * @return Boolean true when success, otherwise false
	 * @throws UnauthorizedException
	 *             when the user is not signed in.
	 * @throws NotFoundException
	 *             when there is no Conference with the given conferenceId.
	 */
	@ApiMethod(name = "registerForConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.POST)

	public WrappedBoolean registerForConference(final User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey)
					throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		// Get the userId
		final String userId = user.getUserId();

		// TODO
		// Start transaction
		WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

			@Override
			public WrappedBoolean run() {
				// TODO Auto-generated method stub
				try {

					// TODO
					// Get the conference key -- you can get it from
					// websafeConferenceKey
					// Will throw ForbiddenException if the key cannot be
					// created
					Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
					Conference conference = ofy().load().key(conferenceKey).now();

					// 404 when there is no Conference with the given
					// conferenceId.
					if (conference == null) {
						return new WrappedBoolean(false, "No Conference found with key: " + websafeConferenceKey);
					}

					// TODO
					// Get the user's Profile entity
					Profile profile =  ofy().load().key(Key.create(Profile.class,userId)).now(); // TODO load
					

					// Has the user already registered to attend this
					// conference?
					if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
						return new WrappedBoolean(false, "Already registered");
					} else if (conference.getSeatsAvailable() <= 0) {
						return new WrappedBoolean(false, "No seats available");
					} else {
						// All looks good, go ahead and book the seat

						// TODO
						// Add the websafeConferenceKey to the profile's
						// conferencesToAttend property
						profile.addToConferenceKeysToAttend(websafeConferenceKey);

						// TODO
						// Decrease the conference's seatsAvailable
						// You can use the bookSeats() method on Conference
						conference.bookSeats(1);
						// TODO
						// Save the Conference and Profile entities
						ofy().save().entities(profile, conference).now();
						// We are booked!
						return new WrappedBoolean(true, "Registration successful");
					}

				} catch (Exception e) {
					return new WrappedBoolean(false, "Unknown exception");
				}

				// return null;
			}
		});

		// if result is false
		if (!result.getResult()) {
			if (result.getReason().contains("No Conference found with key")) {
				throw new NotFoundException(result.getReason());
			} else if (result.getReason() == "Already registered") {
				throw new ConflictException("You have already registered");
			} else if (result.getReason() == "No seats available") {
				throw new ConflictException("There are no seats available");
			} else {
				
				throw new ForbiddenException("Unknown exception");
			}
		}
		return result;
	}

	/**
	 * Returns a collection of Conference Object that the user is going to
	 * attend.
	 *
	 * @param user
	 *            An user who invokes this method, null when the user is not
	 *            signed in.
	 * @return a Collection of Conferences that the user is going to attend.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */
	@ApiMethod(name = "getConferencesToAttend", path = "getConferencesToAttend", httpMethod = HttpMethod.GET)
	public Collection<Conference> getConferencesToAttend(final User user)
			throws UnauthorizedException, NotFoundException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		// TODO
		// Get the Profile entity for the user
		Profile profile = getProfileFromUser(user); // Change this;
		if (profile == null) {
			throw new NotFoundException("Profile doesn't exist.");
		}

		// TODO
		// Get the value of the profile's conferenceKeysToAttend property
		List<String> keyStringsToAttend = profile.getConferenceKeysToAttend(); // change this

		// TODO
		// Iterate over keyStringsToAttend,
		// and return a Collection of the
		// Conference entities that the user has registered to atend
		Collection<Conference> colle = new ArrayList<>(0);
		for(String k: keyStringsToAttend){
			Key<Conference> key = Key.create(k);
			Conference c = ofy().load().key(key).now();
			if(c != null)
				colle.add(c);
		}
		
		return colle; // change this
	}

    @ApiMethod(name = "unregisterFromConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.DELETE)
	public WrappedBoolean unregisterFromConference(final User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey)
			throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		// Get the userId
		final String userId = user.getUserId();

		// Start transaction
		WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
			public WrappedBoolean run() {
				try {

					// Get the conference key -- you can get it from
					// websafeConferenceKey
					// Will throw ForbiddenException if the key cannot be
					// created
					Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

					// Get the Conference entity from the datastore
					Conference conference = ofy().load().key(conferenceKey).now();

					// 404 when there is no Conference with the given
					// conferenceId.
					if (conference == null) {
						return new WrappedBoolean(false, "No Conference found with key: " + websafeConferenceKey);
					}

					// Get the user's Profile entity
					Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

					if (!profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
						return new WrappedBoolean(false, "You are not registred to this conference");
					} else {
						//Unregister user.
						profile.unregisterFromConference(websafeConferenceKey);;

						// Increase the conference's seatsAvailable
						conference.giveBackSeats(1);

						// Save the Conference and Profile entities
						ofy().save().entities(profile, conference).now();
						
						return new WrappedBoolean(true, "Successfully unregistred!");
					}

				} catch (Exception e) {
					return new WrappedBoolean(false, "Unknown exception");
				}
			}
		});
		// if result is false
		if (!result.getResult())

		{
			if (result.getReason().contains("No Conference found with key")) {
				throw new NotFoundException(result.getReason());
			} else if (result.getReason() == "You are not registred to this conference") {
				throw new ConflictException("You are not registred to this conference");
			} else {
				throw new ForbiddenException("Unknown exception");
			}
		}
		return result;
	}

}