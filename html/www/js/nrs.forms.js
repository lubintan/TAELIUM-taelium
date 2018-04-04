/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2017 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of the Nxt software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
	NRS.forms = {};

	$(".modal form input").keydown(function(e) {
		console.log("KEYDOWN");
		
		if (e.which == "13") {
			e.preventDefault();
			if (NRS.settings["submit_on_enter"] && e.target.type != "textarea") {
				$(this).submit();
			} else {
				return false;
			}
		}
	});
	
	NRS.fakeKeydown = function () {
		NRS.forms = {};
		console.log("FAKE KEYDOWN");
		if (NRS.settings["submit_on_enter"] ) {
			$(this).submit();
		} else {
			return false;
		}
	};
	
	NRS.fakeClick = function(){
		NRS.submitForm(null, $(this));
	}
	
	
	

	$(".modal button.btn-primary:not([data-dismiss=modal]):not([data-ignore=true]),button.btn-calculate-fee,button.scan-qr-code,.sendTaels button.btn-wakanda").click(function() {
//		$(".sendTaels button.btn-wakanda").click(function() {
		console.log("LONG FUNCTION");
		
		var $btn = $(this);
		var $modal = $(this).closest(".modal");
        if ($btn.hasClass("scan-qr-code")) {
            var data = $btn.data();
            NRS.scanQRCode(data.reader, function(text) {
                $modal.find("#" + data.result).val(text);
            });
            return;
        }
		try {
			NRS.submitForm2($modal, $btn);
		} catch(e) {
			$modal.find(".error_message").html("Form submission error '" + e.message + "' - please report to developers").show();
			NRS.unlockForm($modal, $btn);
		}
	});

	$(".modal input,select,textarea").change(function() {
        var id = $(this).attr('id');
        var modal = $(this).closest(".modal");
		if (!modal) {
			return;
		}
		var feeFieldId = modal.attr('id');
		if (!feeFieldId) {
			// Not a modal dialog with fee calculation widget
			return;
		}
        feeFieldId = feeFieldId.replace('_modal', '') + "_fee";
        if (id == feeFieldId) {
            return;
        }
        var fee = $("#" + feeFieldId);
        if (fee.val() == "") {
            return;
        }
        var recalcIndicator = $("#" + modal.attr('id').replace('_modal', '') + "_recalc");
        recalcIndicator.show();
    });

	function getSuccessMessage(requestType) {
		var ignore = ["asset_exchange_change_group_name", "asset_exchange_group", "add_contact", "update_contact", "delete_contact",
			"send_message", "decrypt_messages", "start_forging", "stop_forging", "generate_token", "send_money", "set_alias", "add_asset_bookmark", "sell_alias"
		];

		console.log("GET SUCCESS MESSAGE");
		
		if (ignore.indexOf(requestType) != -1) {
			return "";
		} else {
			var key = "success_" + requestType;

			if ($.i18n.exists(key)) {
				return $.t(key);
			} else {
				return "";
			}
		}
	}

	function getErrorMessage(requestType) {
		var ignore = ["start_forging", "stop_forging", "generate_token", "validate_token"];
		console.log("GET ERROR MSG");
		if (ignore.indexOf(requestType) != -1) {
			return "";
		} else {
			var key = "error_" + requestType;

			if ($.i18n.exists(key)) {
				return $.t(key);
			} else {
				return "";
			}
		}
	}

	NRS.addMessageData = function(data, requestType) {
		if (requestType == "sendMessage") {
			data.add_message = true;
		}

		if (!data.add_message && !data.add_note_to_self) {
			delete data.message;
			delete data.note_to_self;
			delete data.encrypt_message;
			delete data.add_message;
			delete data.add_note_to_self;
			return data;
		} else if (!data.add_message) {
			delete data.message;
			delete data.encrypt_message;
			delete data.add_message;
		} else if (!data.add_note_to_self) {
			delete data.note_to_self;
			delete data.add_note_to_self;
		}

		data["_extra"] = {
			"message": data.message,
			"note_to_self": data.note_to_self
		};
		var encrypted;
		var uploadConfig = NRS.getFileUploadConfig("sendMessage", data);
		if ($(uploadConfig.selector)[0].files[0]) {
			data.messageFile = $(uploadConfig.selector)[0].files[0];
		}
		if (data.add_message && (data.message || data.messageFile)) {
			if (data.encrypt_message) {
				try {
					var options = {};
					if (data.recipient) {
						options.account = data.recipient;
					} else if (data.encryptedMessageRecipient) {
						options.account = data.encryptedMessageRecipient;
						delete data.encryptedMessageRecipient;
					}
					if (data.recipientPublicKey) {
						options.publicKey = data.recipientPublicKey;
					}
					if (data.messageFile) {
						// We read the file data and encrypt it later
						data.messageToEncryptIsText = "false";
						data.encryptedMessageIsPrunable = "true";
						data.encryptionKeys = NRS.getEncryptionKeys(options, data.secretPhrase);
					} else {
						if (data.doNotSign) {
							data.messageToEncrypt = data.message;
						} else {
							encrypted = NRS.encryptNote(data.message, options, data.secretPhrase);
							data.encryptedMessageData = encrypted.message;
							data.encryptedMessageNonce = encrypted.nonce;
						}
						data.messageToEncryptIsText = "true";
						if (!data.permanent_message) {
							data.encryptedMessageIsPrunable = "true";
						}
					}
					delete data.message;
				} catch (err) {
					throw err;
				}
			} else {
				if (data.messageFile) {
					data.messageIsText = "false";
					data.messageIsPrunable = "true";
				} else {
					data.messageIsText = "true";
					if (!data.permanent_message && converters.stringToByteArray(data.message).length >= NRS.constants.MIN_PRUNABLE_MESSAGE_LENGTH) {
						data.messageIsPrunable = "true";
					}
				}
			}
		} else {
			delete data.message;
		}

		if (data.add_note_to_self && data.note_to_self) {
			try {
				if (data.doNotSign) {
                    data.messageToEncryptToSelf = data.note_to_self;
                } else {
                    encrypted = NRS.encryptNote(data.note_to_self, {
                        "publicKey": converters.hexStringToByteArray(NRS.generatePublicKey(data.secretPhrase))
                    }, data.secretPhrase);

                    data.encryptToSelfMessageData = encrypted.message;
                    data.encryptToSelfMessageNonce = encrypted.nonce;
                }
				data.messageToEncryptToSelfIsText = "true";
				delete data.note_to_self;
			} catch (err) {
				throw err;
			}
		} else {
			delete data.note_to_self;
		}
		delete data.add_message;
		delete data.add_note_to_self;
		return data;
	};

	
	 NRS.submitForm2 = function($modal, $btn) {
			if (!$btn) {
				console.log("NO BUTTON");
				$btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
			}
			
			console.log("SUBMIT FORM");

//			$modal = $btn.closest(".modal");
			$modal = $btn.closest(".sendTaels");

			$modal.modal("lock");
			$modal.find("button").prop("disabled", true);
			$btn.button("loading");
	
	$form = $modal.find("form:first");
	
	var requestType = $form.find("input[name=request_type]").val();
	
	
	
	var requestTypeKey = requestType.replace(/([A-Z])/g, function($1) {
				console.log("ff");
				return "_" + $1.toLowerCase();
			});
	
	

			var successMessage = getSuccessMessage(requestTypeKey);
			var errorMessage = getErrorMessage(requestTypeKey);

			var data = null;

			var formFunction = NRS["forms"][requestType];
			var formErrorFunction = NRS["forms"][requestType + "Error"];

			if (typeof formErrorFunction != "function") {
				console.log("gg");
				formErrorFunction = false;
			}

			var originalRequestType = requestType;
	        if (NRS.isRequireBlockchain(requestType)) {
	        	console.log("hh");
	        	
	    		if (NRS.downloadingBlockchain && !NRS.state.apiProxy) {
					console.log("ii");
					$form.find(".error_message").html($.t("error_blockchain_downloading")).show();
					if (formErrorFunction) {
						formErrorFunction();
					}
					NRS.unlockForm($modal, $btn);
					return;
				} else if (NRS.state.isScanning) {
					console.log("jj");
					$form.find(".error_message").html($.t("error_form_blockchain_rescanning")).show();
					if (formErrorFunction) {
						formErrorFunction();
					}
					NRS.unlockForm($modal, $btn);
					return;
				}
	        	
	        }


			var invalidElement = false;

			//TODO
			$form.find(":input").each(function() {
				console.log("kk");
				if ($(this).is(":invalid")) {
				$form.find(".error_message").html(error).show();
				NRS.unlockForm($modal, $btn);
					invalidElement = true;
					return false;
				}
			});

			if (invalidElement) {
				console.log("qq");
				return;
			}

			
				
				data = NRS.getFormData($form);
				
				console.log("form: " + data);
//				for (var name in $form){
//				console.log(name);
//				}
//				
				
				console.log("data: " + data);
				for (var name in data){
					console.log(name);
					}

	        		console.log("yy");
	            delete data.calculateFee;
	            if (!data.feeNXT) {
	            	console.log("zz");
                data.feeNXT = "0";
	            
	        }

			if (data.recipient) {
				console.log("AA");
				data.recipient = $.trim(data.recipient);
			}

			if (requestType == "sendMoney" || requestType == "transferAsset") {
				console.log("BB");
				var merchantInfo = $modal.find("input[name=merchant_info]").val();
				
			}
			try {
				console.log("CC");
				data = NRS.addMessageData(data, requestType);
			} catch (err) {
				console.log("CC1");
				$form.find(".error_message").html(String(err.message).escapeHTML()).show();
				if (formErrorFunction) {
					console.log("CC2");
					formErrorFunction();
				}
				NRS.unlockForm($modal, $btn);
				return;
			}

			if (data.deadline) {
				console.log("DD");
				data.deadline = String(data.deadline * 60); //hours to minutes
			}

			if (!NRS.showedFormWarning) {
				console.log("FF");
				if ("amountNXT" in data && NRS.settings["amount_warning"] && NRS.settings["amount_warning"] != "0") {
					console.log("FF1");
					try {
						var amountNQT = NRS.convertToNQT(data.amountNXT);
					} catch (err) {
						console.log("FF2");
						$form.find(".error_message").html(String(err).escapeHTML() + " (" + $.t("amount") + ")").show();
						if (formErrorFunction) {
							console.log("FF3");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
						return;
					}
				}

				if ("feeNXT" in data && NRS.settings["fee_warning"] && NRS.settings["fee_warning"] != "0") {
					console.log("HH");
					try {
						var feeNQT = NRS.convertToNQT(data.feeNXT);
					} catch (err) {
						console.log("HH1");
						$form.find(".error_message").html(String(err).escapeHTML() + " (" + $.t("fee") + ")").show();
						if (formErrorFunction) {
							console.log("HH2");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
						return;
					}
				}
			}

			console.log("************** " + requestType);
			console.log("data feeNXT: " + data["feeNXT"]);
			console.log("data amountNXT: " + data["amountNXT"]);
			console.log("data minBalanceNXT: " + data["minBalanceNXT"]);
			console.log("data recipient: " + data.recipient);
			console.log("data deadline: " + data.deadline);
			
			
				NRS.sendRequest(requestType, data, function (response) {
					formResponse(response, data, requestType, $modal, $form, $btn, successMessage,
						originalRequestType, formErrorFunction, errorMessage);
				});
			};
	
	
    NRS.submitForm = function($modal, $btn) {
		if (!$btn) {
			$btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
		}
		
		console.log("SUBMIT FORM");

		$modal = $btn.closest(".modal");

		$modal.modal("lock");
		$modal.find("button").prop("disabled", true);
		$btn.button("loading");

//		console.log("$btn: " + $btn.text());
		console.log("$btn: " + $btn.data());
		
//		for (var name in $btn){
//			console.log(name);
//		}
		
		
//		console.log("$modal: " + $modal.text());
		console.log("$modal: " + $modal.val());
//		for (var name in $modal){
//			console.log(name);
//		}
		
        var $form;
        
        console.log("$btn.data(form) " +$btn.data("form") );
        
		if ($btn.data("form")) {
			$form = $modal.find("form#" + $btn.data("form"));
			console.log("aa");
			
			if (!$form.length) {
				$form = $modal.find("form:first");
				console.log("bb");
			}
		} else {
			$form = $modal.find("form:first");
			console.log("cc");
		}

		var requestType;
		if ($btn.data("request")) {
			console.log("dd");
			requestType = $btn.data("request");
		} else {
			console.log("ee");
			requestType = $form.find("input[name=request_type]").val();
		}
		var requestTypeKey = requestType.replace(/([A-Z])/g, function($1) {
			console.log("ff");
			return "_" + $1.toLowerCase();
		});

		var successMessage = getSuccessMessage(requestTypeKey);
		var errorMessage = getErrorMessage(requestTypeKey);

		var data = null;

		var formFunction = NRS["forms"][requestType];
		var formErrorFunction = NRS["forms"][requestType + "Error"];

		if (typeof formErrorFunction != "function") {
			console.log("gg");
			formErrorFunction = false;
		}

		var originalRequestType = requestType;
        if (NRS.isRequireBlockchain(requestType)) {
        	console.log("hh");
			if (NRS.downloadingBlockchain && !NRS.state.apiProxy) {
				console.log("ii");
				$form.find(".error_message").html($.t("error_blockchain_downloading")).show();
				if (formErrorFunction) {
					formErrorFunction();
				}
				NRS.unlockForm($modal, $btn);
				return;
			} else if (NRS.state.isScanning) {
				console.log("jj");
				$form.find(".error_message").html($.t("error_form_blockchain_rescanning")).show();
				if (formErrorFunction) {
					formErrorFunction();
				}
				NRS.unlockForm($modal, $btn);
				return;
			}
		}

		var invalidElement = false;

		//TODO
		$form.find(":input").each(function() {
			console.log("kk");
			if ($(this).is(":invalid")) {
				console.log("ll");
				var error = "";
				var name = String($(this).attr("name")).replace("NXT", "").replace("NQT", "").capitalize();
				var value = $(this).val();

				if ($(this).hasAttr("max")) {
					console.log("mm");
					if (!/^[\-\d\.]+$/.test(value)) {
						error = $.t("error_not_a_number", {
							"field": NRS.getTranslatedFieldName(name).toLowerCase()
						}).capitalize();
					} else {
						console.log("nn");
						var max = $(this).attr("max");

						if (value > max) {
							error = $.t("error_max_value", {
								"field": NRS.getTranslatedFieldName(name).toLowerCase(),
								"max": max
							}).capitalize();
						}
					}
				}

				if ($(this).hasAttr("min")) {
					if (!/^[\-\d\.]+$/.test(value)) {
						error = $.t("error_not_a_number", {
							"field": NRS.getTranslatedFieldName(name).toLowerCase()
						}).capitalize();
					} else {
						var min = $(this).attr("min");

						if (value < min) {
							error = $.t("error_min_value", {
								"field": NRS.getTranslatedFieldName(name).toLowerCase(),
								"min": min
							}).capitalize();
						}
					}
				}

				if (!error) {
					console.log("oo");
					error = $.t("error_invalid_field", {
						"field": NRS.getTranslatedFieldName(name).toLowerCase()
					}).capitalize();
				}

				$form.find(".error_message").html(error).show();

				if (formErrorFunction) {
					console.log("pp");
					formErrorFunction();
				}

				NRS.unlockForm($modal, $btn);
				invalidElement = true;
				return false;
			}
		});

		if (invalidElement) {
			console.log("qq");
			return;
		}

		if (typeof formFunction == "function") {
			console.log("rr");
			var output = formFunction($modal);

			if (!output) {
				console.log("ss");
				return;
			} else if (output.error) {
				console.log("tt");
				$form.find(".error_message").html(output.error.escapeHTML()).show();
				if (formErrorFunction) {
					console.log("uu");
					formErrorFunction();
				}
				NRS.unlockForm($modal, $btn);
				return;
			} else {
				console.log("vv");
				if (output.requestType) {
					console.log("v1");
					requestType = output.requestType;
				}
				if (output.data) {
					console.log("v2");
					data = output.data;
				}
				if ("successMessage" in output) {
					console.log("v3");
					successMessage = output.successMessage;
				}
				if ("errorMessage" in output) {
					console.log("v4");
					errorMessage = output.errorMessage;
				}
				if (output.stop) {
					console.log("v5");
					if (errorMessage) {
						$form.find(".error_message").html(errorMessage).show();
					} else if (successMessage) {
						$.growl(successMessage.escapeHTML(), {
							type: "success"
						});
					}
					NRS.unlockForm($modal, $btn, !output.keepOpen);
					return;
				}
				if (output.reload) {
					console.log("v6");
					window.location.reload(output.forceGet);
					return;
				}
			}
		}

		if (!data) {
			console.log("ww");
			data = NRS.getFormData($form);
		}
        if ($btn.hasClass("btn-calculate-fee")) {
        	console.log("xx");
            data.calculateFee = true;
            data.feeNXT = "0";
            $form.find(".error_message").html("").hide();
        } else {
        	console.log("yy");
            delete data.calculateFee;
            if (!data.feeNXT) {
            	console.log("zz");
                data.feeNXT = "0";
            }
        }

		if (data.recipient) {
			console.log("AA");
			data.recipient = $.trim(data.recipient);
			if (NRS.isNumericAccount(data.recipient)) {
				console.log("AA1");
				$form.find(".error_message").html($.t("error_numeric_ids_not_allowed")).show();
				if (formErrorFunction) {
					console.log("AA2");
					formErrorFunction(false, data);
				}
				NRS.unlockForm($modal, $btn);
				return;
			} else if (!NRS.isRsAccount(data.recipient)) {
				console.log("AA3");
				var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
				if (!convertedAccountId || (!NRS.isNumericAccount(convertedAccountId) && !NRS.isRsAccount(convertedAccountId))) {
					console.log("AA4");
					$form.find(".error_message").html($.t("error_account_id")).show();
					if (formErrorFunction) {
						console.log("AA5");
						formErrorFunction(false, data);
					}
					NRS.unlockForm($modal, $btn);
					return;
				} else {
					console.log("AA6");
					data.recipient = convertedAccountId;
					data["_extra"] = {
						"convertedAccount": true
					};
				}
			}
		}

		if (requestType == "sendMoney" || requestType == "transferAsset") {
			console.log("BB");
			var merchantInfo = $modal.find("input[name=merchant_info]").val();
			if (merchantInfo) {
				console.log("BB1");
				var result = merchantInfo.match(/#merchant:(.*)#/i);

				if (result && result[1]) {
					console.log("BB2");
					merchantInfo = $.trim(result[1]);

					if (!data.add_message || !data.message) {
						console.log("BB3");
						$form.find(".error_message").html($.t("info_merchant_message_required")).show();
						if (formErrorFunction) {
							console.log("BB4");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
						return;
					}

					if (merchantInfo == "numeric") {
						console.log("BB5");
						merchantInfo = "[0-9]+";
					} else if (merchantInfo == "alphanumeric") {
						console.log("BB6");
						merchantInfo = "[a-zA-Z0-9]+";
					}

					var regexParts = merchantInfo.match(/^\/(.*?)\/(.*)$/);

					if (!regexParts) {
						console.log("BB7");
						regexParts = ["", merchantInfo, ""];
					}

					var strippedRegex = regexParts[1].replace(/^[\^\(]*/, "").replace(/[\$\)]*$/, "");

					if (regexParts[1].charAt(0) != "^") {
						console.log("BB8");
						regexParts[1] = "^" + regexParts[1];
					}

					if (regexParts[1].slice(-1) != "$") {
						console.log("BB9");
						regexParts[1] = regexParts[1] + "$";
					}
                    var regexp;
					if (regexParts[2].indexOf("i") !== -1) {
						console.log("BB10");
						regexp = new RegExp(regexParts[1], "i");
					} else {
						console.log("BB11");
						regexp = new RegExp(regexParts[1]);
					}

					if (!regexp.test(data.message)) {
						console.log("BB12");
						var regexType;
						errorMessage = "";
						var lengthRequirement = strippedRegex.match(/\{(.*)\}/);

						if (lengthRequirement) {
							console.log("BB13");
							strippedRegex = strippedRegex.replace(lengthRequirement[0], "+");
						}

						if (strippedRegex == "[0-9]+") {
							console.log("BB14");
							regexType = "numeric";
						} else if (strippedRegex == "[a-z0-9]+" || strippedRegex.toLowerCase() == "[a-za-z0-9]+" || strippedRegex == "[a-z0-9]+") {
							console.log("BB15");
							regexType = "alphanumeric";
						} else {
							console.log("BB16");
							regexType = "custom";
						}

						if (lengthRequirement) {
							console.log("BB17");
							if (lengthRequirement[1].indexOf(",") != -1) {
								console.log("BB18");
								lengthRequirement = lengthRequirement[1].split(",");
								var minLength = parseInt(lengthRequirement[0], 10);
								if (lengthRequirement[1]) {
									console.log("BB19");
									var maxLength = parseInt(lengthRequirement[1], 10);
									errorMessage = $.t("error_merchant_message_" + regexType + "_range_length", {
										"minLength": minLength,
										"maxLength": maxLength
									});
								} else {
									console.log("BB20");
									errorMessage = $.t("error_merchant_message_" + regexType + "_min_length", {
										"minLength": minLength
									});
								}
							} else {
								console.log("BB21");
								var requiredLength = parseInt(lengthRequirement[1], 10);
								errorMessage = $.t("error_merchant_message_" + regexType + "_length", {
									"length": requiredLength
								});
							}
						} else {
							console.log("BB22");
							errorMessage = $.t("error_merchant_message_" + regexType);
						}

						$form.find(".error_message").html(errorMessage).show();
						if (formErrorFunction) {
							console.log("BB23");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
						return;
					}
				}
			}
		}
		try {
			console.log("CC");
			data = NRS.addMessageData(data, requestType);
		} catch (err) {
			console.log("CC1");
			$form.find(".error_message").html(String(err.message).escapeHTML()).show();
			if (formErrorFunction) {
				console.log("CC2");
				formErrorFunction();
			}
			NRS.unlockForm($modal, $btn);
			return;
		}

		if (data.deadline) {
			console.log("DD");
			data.deadline = String(data.deadline * 60); //hours to minutes
		}

        if ("secretPhrase" in data && !data.secretPhrase.length && !NRS.rememberPassword &&
                !(data.calculateFee && NRS.accountInfo.publicKey)) {
        	console.log("EE");
			$form.find(".error_message").html($.t("error_passphrase_required")).show();
			if (formErrorFunction) {
				console.log("EE1");
				formErrorFunction(false, data);
			}
            $("#" + $modal.attr('id').replace('_modal', '') + "_password").focus();
			NRS.unlockForm($modal, $btn);
			return;
		}

		if (!NRS.showedFormWarning) {
			console.log("FF");
			if ("amountNXT" in data && NRS.settings["amount_warning"] && NRS.settings["amount_warning"] != "0") {
				console.log("FF1");
				try {
					var amountNQT = NRS.convertToNQT(data.amountNXT);
				} catch (err) {
					console.log("FF2");
					$form.find(".error_message").html(String(err).escapeHTML() + " (" + $.t("amount") + ")").show();
					if (formErrorFunction) {
						console.log("FF3");
						formErrorFunction(false, data);
					}
					NRS.unlockForm($modal, $btn);
					return;
				}

				if (new BigInteger(amountNQT).compareTo(new BigInteger(NRS.settings["amount_warning"])) > 0) {
					console.log("GG");
					NRS.showedFormWarning = true;
					$form.find(".error_message").html($.t("error_max_amount_warning", {
						"amount": NRS.formatAmount(NRS.settings["amount_warning"]), "symbol": NRS.constants.COIN_SYMBOL
					})).show();
					if (formErrorFunction) {
						console.log("GG1");
						formErrorFunction(false, data);
					}
					NRS.unlockForm($modal, $btn);
					return;
				}
			}

			if ("feeNXT" in data && NRS.settings["fee_warning"] && NRS.settings["fee_warning"] != "0") {
				console.log("HH");
				try {
					var feeNQT = NRS.convertToNQT(data.feeNXT);
				} catch (err) {
					console.log("HH1");
					$form.find(".error_message").html(String(err).escapeHTML() + " (" + $.t("fee") + ")").show();
					if (formErrorFunction) {
						console.log("HH2");
						formErrorFunction(false, data);
					}
					NRS.unlockForm($modal, $btn);
					return;
				}

				if (new BigInteger(feeNQT).compareTo(new BigInteger(NRS.settings["fee_warning"])) > 0) {
					console.log("II");
					NRS.showedFormWarning = true;
					$form.find(".error_message").html($.t("error_max_fee_warning", {
						"amount": NRS.formatAmount(NRS.settings["fee_warning"]), "symbol": NRS.constants.COIN_SYMBOL
					})).show();
					if (formErrorFunction) {
						console.log("II1");
						formErrorFunction(false, data);
					}
					NRS.unlockForm($modal, $btn);
					return;
				}
			}

			if ("decimals" in data) {
				console.log("JJ");
                try {
                    var decimals = parseInt(data.decimals);
				} catch (err) {
					console.log("JJ1");
                    decimals = 0;
				}

				if (decimals < 2 || decimals > 6) {
					console.log("JJ2");
					if (requestType == "issueAsset" && data.quantityQNT == "1") {
						console.log("J3");
						// Singleton asset no need to warn
					} else {
						console.log("JJ4");
						NRS.showedFormWarning = true;
						var entity = (requestType == 'issueCurrency' ? 'currency' : 'asset');
						$form.find(".error_message").html($.t("error_decimal_positions_warning", {
							"entity": entity
						})).show();
						if (formErrorFunction) {
							console.log("JJ5");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
						return;
					}
				}
			}

			var convertNXTFields = ["phasingQuorumNXT", "phasingMinBalanceNXT"];
			$.each(convertNXTFields, function(key, field) {
				if (field in data) {
					console.log("KK");
					try {
						NRS.convertToNQT(data[field]);
					} catch (err) {
						console.log("KK1");
						$form.find(".error_message").html(String(err).escapeHTML()).show();
						if (formErrorFunction) {
							console.log("KK2");
							formErrorFunction(false, data);
						}
						NRS.unlockForm($modal, $btn);
					}
				}
			});
		}

		if (data.doNotBroadcast || data.calculateFee) {
			console.log("LL");
			data.broadcast = "false";
            if (data.calculateFee) {
            	console.log("LL1");
                if (NRS.accountInfo.publicKey) {
                	console.log("LL2");
                    data.publicKey = NRS.accountInfo.publicKey;
                    delete data.secretPhrase;
                }
            }
            if (data.doNotBroadcast) {
            	console.log("MM");
                delete data.doNotBroadcast;
            }
		}
		if (data.messageFile && data.encrypt_message) {
			console.log("NN");
			if (!NRS.isFileEncryptionSupported()) {
				console.log("NN1");
                $form.find(".error_message").html($.t("file_encryption_not_supported")).show();
                if (formErrorFunction) {
                	console.log("NN2");
                    formErrorFunction(false, data);
                }
                NRS.unlockForm($modal, $btn);
                return;
            }
			try {
				console.log("OO");
				NRS.encryptFile(data.messageFile, data.encryptionKeys, function(encrypted) {
					data.messageFile = encrypted.file;
					data.encryptedMessageNonce = converters.byteArrayToHexString(encrypted.nonce);
					delete data.encryptionKeys;

					NRS.sendRequest(requestType, data, function (response) {
						formResponse(response, data, requestType, $modal, $form, $btn, successMessage,
							originalRequestType, formErrorFunction, errorMessage);
					})
				});
			} catch (err) {
				console.log("OO1");
				$form.find(".error_message").html(String(err).escapeHTML()).show();
				if (formErrorFunction) {
					console.log("OO2");
					formErrorFunction(false, data);
				}
				NRS.unlockForm($modal, $btn);
			}
		} else {
			console.log("OO3");
			NRS.sendRequest(requestType, data, function (response) {
				formResponse(response, data, requestType, $modal, $form, $btn, successMessage,
					originalRequestType, formErrorFunction, errorMessage);
			});
		}
	};// end submit form

	function formResponse(response, data, requestType, $modal, $form, $btn, successMessage,
						  originalRequestType, formErrorFunction, errorMessage) {
		//todo check again.. response.error
		var formCompleteFunction;
		
		console.log("Form Response");
		console.log("response tx: " + response.transaction);
		console.log(response.fullHash);
		
		if (response.fullHash) {
			NRS.unlockForm($modal, $btn);
			if (data.calculateFee) {
				updateFee($modal, response.transactionJSON.feeNQT);
				return;
			}

			if (!$modal.hasClass("modal-no-hide")) {
				$modal.modal("hide");
			}

			if (successMessage) {
				console.log(successMessage);
				$.growl(successMessage.escapeHTML(), {
					type: "success"
				});

			}

			formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];
			

			if (requestType != "parseTransaction" && requestType != "calculateFullHash") {
				if (typeof formCompleteFunction == "function") {
					data.requestType = requestType;

					if (response.transaction) {
						NRS.addUnconfirmedTransaction(response.transaction, function(alreadyProcessed) {
							response.alreadyProcessed = alreadyProcessed;
							formCompleteFunction(response, data);
							$form[0].reset();
							NRS.loadWoGoingToPage("dashboard");
						});
					} else {
						response.alreadyProcessed = false;
						formCompleteFunction(response, data);
						$form[0].reset();
						NRS.loadWoGoingToPage("dashboard");
					}
				} else {
					NRS.addUnconfirmedTransaction(response.transaction);
				}
			} else {
				if (typeof formCompleteFunction == "function") {
					data.requestType = requestType;
					formCompleteFunction(response, data);
					$form[0].reset();
					NRS.loadWoGoingToPage("dashboard");
				}
			}

		} else if (response.errorCode) {
			$form.find(".error_message").html(NRS.escapeRespStr(response.errorDescription)).show();

			if (formErrorFunction) {
				formErrorFunction(response, data);
			}

			NRS.unlockForm($modal, $btn);
		} else {
			if (data.calculateFee) {
				NRS.unlockForm($modal, $btn, false);
				updateFee($modal, response.transactionJSON.feeNQT);
				return;
			}
			var sentToFunction = false;
			if (!errorMessage) {
				formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];

				if (typeof formCompleteFunction == 'function') {
					sentToFunction = true;
					data.requestType = requestType;

					NRS.unlockForm($modal, $btn);

					if (!$modal.hasClass("modal-no-hide")) {
						$modal.modal("hide");
					}
					formCompleteFunction(response, data);
					$form[0].reset();
					NRS.loadWoGoingToPage("dashboard");
				} else {
					errorMessage = $.t("error_unknown");
				}
			}
			if (!sentToFunction) {
				NRS.unlockForm($modal, $btn, true);

				$.growl(errorMessage.escapeHTML(), {
					type: 'danger'
				});
			}
		}
	} //end form response

    NRS.lockForm = function($modal) {
        $modal.find("button").prop("disabled", true);
        var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
        if ($btn) {
            $btn.button("loading");
        }
        $modal.modal("lock");
        
        console.log("LOCK FORM");
        
        return $btn;
    };


	NRS.unlockForm = function($modal, $btn, hide) {
		$modal.find("button").prop("disabled", false);
		if ($btn) {
			$btn.button("reset");
		}
		$modal.modal("unlock");
		if (hide) {
			$modal.modal("hide");
		}
		
		console.log("UNLOCK FORM");
	};

    function updateFee(modal, feeNQT) {
        var fee = $("#" + modal.attr('id').replace('_modal', '') + "_fee");
        fee.val(NRS.convertToNXT(feeNQT));
        var recalcIndicator = $("#" + modal.attr('id').replace('_modal', '') + "_recalc");
        recalcIndicator.hide();
    }

	return NRS;
}(NRS || {}, jQuery));