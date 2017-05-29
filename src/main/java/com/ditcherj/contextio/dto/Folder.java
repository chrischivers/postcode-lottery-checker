package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Author: Jonathan Ditcher
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Folder {

	private String name;
	private String symbolic_name;
	private Object attributes;
	private String delim;
	private Integer nb_messages;
	private Integer nb_unseen_messages;
	private Integer uidvalidity;
	private String resource_url;

	public Folder() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getAttributes() {
		return attributes;
	}

	public void setAttributes(Object attributes) {
		this.attributes = attributes;
	}

	public String getDelim() {
		return delim;
	}

	public void setDelim(String delim) {
		this.delim = delim;
	}

	public Integer getNb_messages() {
		return nb_messages;
	}

	public void setNb_messages(Integer nb_messages) {
		this.nb_messages = nb_messages;
	}

	public Integer getNb_unseen_messages() {
		return nb_unseen_messages;
	}

	public void setNb_unseen_messages(Integer nb_unseen_messages) {
		this.nb_unseen_messages = nb_unseen_messages;
	}

	public Integer getUidvalidity() {
		return uidvalidity;
	}

	public void setUidvalidity(Integer uidvalidity) {
		this.uidvalidity = uidvalidity;
	}

	public String getResource_url() {
		return resource_url;
	}

	public void setResource_url(String resource_url) {
		this.resource_url = resource_url;
	}

	public String getSymbolic_name() {
		return symbolic_name;
	}

	public void setSymbolic_name(String symbolic_name) {
		this.symbolic_name = symbolic_name;
	}

	@Override
	public String toString() {
		return "Folder{" +
				"name='" + name + '\'' +
				", symbolic_name='" + symbolic_name + '\'' +
				", attributes=" + attributes +
				", delim='" + delim + '\'' +
				", nb_messages=" + nb_messages +
				", nb_unseen_messages=" + nb_unseen_messages +
				", uidvalidity=" + uidvalidity +
				", resource_url='" + resource_url + '\'' +
				'}';
	}

	public static class Attributes{

		private Boolean hasChildren;
		private Boolean hasNoChildren;
		private Boolean noinferiors;
		private Boolean marked;
		private Boolean noselect;

		public Attributes() {
		}

		public Boolean getHasChildren() {
			return hasChildren;
		}

		public void setHasChildren(Boolean hasChildren) {
			this.hasChildren = hasChildren;
		}

		public Boolean getHasNoChildren() {
			return hasNoChildren;
		}

		public void setHasNoChildren(Boolean hasNoChildren) {
			this.hasNoChildren = hasNoChildren;
		}

		public Boolean getNoinferiors() {
			return noinferiors;
		}

		public void setNoinferiors(Boolean noinferiors) {
			this.noinferiors = noinferiors;
		}

		public Boolean getMarked() {
			return marked;
		}

		public void setMarked(Boolean marked) {
			this.marked = marked;
		}

		public Boolean getNoselect() {
			return noselect;
		}

		public void setNoselect(Boolean noselect) {
			this.noselect = noselect;
		}

		@Override
		public String toString() {
			return "Attributes{" +
					"hasChildren=" + hasChildren +
					", hasNoChildren=" + hasNoChildren +
					", noinferiors=" + noinferiors +
					", marked=" + marked +
					", noselect=" + noselect +
					'}';
		}
	}
}
