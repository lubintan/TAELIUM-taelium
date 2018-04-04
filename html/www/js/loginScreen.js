/**
 * @depends {nrs.js}
 */

var NRS = (function(NRS, $, undefined) {
	NRS.loginWithPassword = function(){
		$("#lockscreen").hide();
		$("body, html").removeClass("lockscreen");
		$("#login_error").html("").hide();
		NRS.goToPage("passwordLogin");
	};
});