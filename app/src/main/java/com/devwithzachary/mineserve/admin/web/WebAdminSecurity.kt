package com.devwithzachary.mineserve.admin.web

import com.devwithzachary.mineserve.auth.WebAuthSession
import com.devwithzachary.mineserve.auth.WebSessionManager

typealias AdminSession = WebAuthSession

class WebAdminSecurity : WebSessionManager()
