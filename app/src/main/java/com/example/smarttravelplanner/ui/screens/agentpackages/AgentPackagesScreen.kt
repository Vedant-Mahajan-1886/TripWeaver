package com.example.smarttravelplanner.ui.screens.agentpackages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.util.Locale

/** Agent's "My packages": drafts and published ready-made trips. */
@Composable
fun AgentPackagesScreen(
    onBack: () -> Unit,
    onEdit: (packageId: Long) -> Unit,
    viewModel: AgentPackagesViewModel = hiltViewModel()
) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<AgentPackage?>(null) }

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(0L) },
                containerColor = Amber,
                contentColor = Color(0xFF1A1A1A),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New package", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("My packages", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
            }

            val list = packages
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amber) }
                list.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Luggage, contentDescription = null, tint = OnBackgroundGray)
                    Spacer(Modifier.height(8.dp))
                    Text("No packages yet", fontFamily = DmSansFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("Create a ready-made trip travellers can book in one tap.", fontFamily = DmSansFontFamily,
                        fontSize = 13.sp, color = OnBackgroundGray)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { pkg ->
                        PackageCard(
                            pkg = pkg,
                            onEdit = { onEdit(pkg.id) },
                            onTogglePublished = { viewModel.togglePublished(pkg) },
                            onDelete = { deleting = pkg }
                        )
                    }
                }
            }
        }
    }

    deleting?.let { pkg ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \u201C${pkg.title}\u201D?") },
            text = { Text("Travellers won't see it any more. Trips already booked from it are not affected.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(pkg); deleting = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }
        )
    }
}

@Composable
private fun PackageCard(
    pkg: AgentPackage,
    onEdit: () -> Unit,
    onTogglePublished: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pkg.title, fontFamily = PlayfairFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${pkg.destination} \u00B7 ${pkg.dayCount} day${if (pkg.dayCount == 1) "" else "s"} \u00B7 " +
                        "\u20B9" + String.format(Locale.US, "%,d", pkg.pricePerPerson.toLong()) + " / person",
                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                )
            }
            val color = if (pkg.isPublished) Color(0xFF4ADE80) else OnBackgroundGray
            Text(
                if (pkg.isPublished) "Published" else "Draft",
                fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visible to travellers", fontFamily = DmSansFontFamily, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = pkg.isPublished,
                onCheckedChange = { onTogglePublished() },
                colors = SwitchDefaults.colors(checkedTrackColor = Amber)
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Amber) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
